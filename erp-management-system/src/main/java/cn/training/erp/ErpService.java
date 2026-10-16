package cn.training.erp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.math.*;
import java.time.*;
import java.util.*;
import static cn.training.erp.Models.*;

@Service
public class ErpService {
    private final ErpMapper db;
    private final ObjectMapper json;
    private final TrainingMapper training;
    private final WorkflowService workflow;
    @Value("${erp.workflow.required:true}") private boolean approvalRequired;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    public ErpService(ErpMapper db,ObjectMapper json,TrainingMapper training,WorkflowService workflow) {
        this.db=db;this.json=json;this.training=training;this.workflow=workflow;
    }

    Map<String,Object> asMap(Object record) {
        return json.convertValue(record, new TypeReference<LinkedHashMap<String,Object>>() {});
    }
    static BigDecimal decimal(Map<String,Object> row, String key) {
        return new BigDecimal(row.get(key).toString());
    }
    static long number(Map<String,Object> row, String key) { return ((Number)row.get(key)).longValue(); }
    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
    static Map<String,Object> existing(Map<String,Object> row, String message) {
        require(row!=null,message); return row;
    }
    static BigDecimal money(BigDecimal value) { return value.setScale(2,RoundingMode.HALF_UP); }

    @Transactional
    public Map<String,Object> saveProduct(Long id, ProductInput input) {
        Map<String,Object> row=asMap(input);
        row.put("sku",input.sku().trim().toUpperCase(Locale.ROOT));
        row.put("name",input.name().trim());
        if (id==null) db.insertProduct(row);
        else {
            var old=existing(db.lockProduct(id),"商品不存在");
            require(old.get("sku").equals(row.get("sku")),"商品编码创建后不能修改");
            require(old.get("unit").equals(input.unit()),"商品单位创建后不能修改");
            row.put("id",id); db.updateProduct(row);
        }
        workflow.audit(id==null?"新增商品":"修改商品",row.get("sku").toString(),row.get("name").toString());
        return db.product(number(row,"id"));
    }

    @Transactional
    public Map<String,Object> addPartner(PartnerInput input) {
        Map<String,Object> row=asMap(input);
        row.put("name",input.name().trim());
        db.insertPartner(row);
        workflow.audit("新增往来单位",row.get("name").toString(),input.type());
        return db.partner(number(row,"id"));
    }

    @Transactional
    public Map<String,Object> createOrder(OrderInput input) {
        require(!input.businessDate().isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai"))),"业务日期不能晚于今天");
        Map<String,Object> partner=existing(db.partner(input.partnerId()),"往来单位不存在");
        require(partner.get("type").equals(input.type().equals("PURCHASE")?"SUPPLIER":"CUSTOMER"),"往来单位类型与单据不匹配");
        Set<Long> seen=new HashSet<>();
        BigDecimal total=ZERO;
        List<Map<String,Object>> lines=new ArrayList<>();
        for (LineInput line:input.lines()) {
            require(seen.add(line.productId()),"同一张单据中商品不能重复，请合并数量");
            existing(db.product(line.productId()),"商品不存在");
            BigDecimal amount=money(line.quantity().multiply(line.unitPrice()));
            require(amount.precision()-amount.scale()<=14,"单行金额过大");
            Map<String,Object> values=asMap(line);
            values.put("lineAmount",amount); lines.add(values); total=total.add(amount);
        }
        require(total.compareTo(new BigDecimal("99999999999999.99"))<=0,"单据总金额过大");
        Map<String,Object> row=asMap(input);
        String prefix=input.type().equals("PURCHASE")?"CG":"XS";
        row.put("orderNo",prefix+input.businessDate().toString().replace("-","")+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT));
        row.put("totalAmount",total);
        db.insertOrder(row);
        long id=number(row,"id");
        for (var line:lines) { line.put("orderId",id); db.insertLine(line); }
        training.createWorkflow(id);
        workflow.audit("创建单据",row.get("orderNo").toString(),"金额 "+total);
        return detail(id);
    }

    public Map<String,Object> detail(long id) {
        Map<String,Object> order=existing(db.order(id),"单据不存在");
        order.put("lines",db.lines(id));
        order.put("workflow",training.workflow(id));
        return order;
    }

    @Transactional
    public Map<String,Object> post(long id) {
        var order=existing(db.lockOrder(id),"单据不存在");
        require(order.get("status").equals("DRAFT"),"只有待处理单据可以入库或出库，请刷新页面");
        if(approvalRequired){
            var flow=training.workflow(id);
            require(flow!=null&&flow.get("state").equals("APPROVED"),"请先提交单据并完成审批");
        }
        boolean purchase=order.get("type").equals("PURCHASE");
        // Lock products in a fixed order so concurrent orders cannot oversell or deadlock.
        for (var line:db.lines(id)) {
            long productId=number(line,"product_id");
            var product=existing(db.lockProduct(productId),"商品不存在");
            BigDecimal quantity=decimal(line,"quantity"), stock=decimal(product,"stock");
            BigDecimal average=decimal(product,"average_cost"), price=decimal(line,"unit_price");
            BigDecimal nextStock=purchase?stock.add(quantity):stock.subtract(quantity);
            require(nextStock.signum()>=0,product.get("name")+" 库存不足，当前可用 "+stock.stripTrailingZeros().toPlainString()+" "+product.get("unit"));
            require(nextStock.compareTo(new BigDecimal("999999999999999.999"))<=0,"库存数量超过上限");
            BigDecimal nextAverage=purchase
                ?stock.multiply(average).add(quantity.multiply(price)).divide(nextStock,6,RoundingMode.HALF_UP)
                :average;
            db.updateStock(productId,nextStock,nextAverage);
            db.updateLineCost(number(line,"id"),money(quantity.multiply(purchase?price:average)));
            Map<String,Object> movement=new HashMap<>();
            movement.put("productId",productId); movement.put("orderId",id);
            movement.put("type",order.get("type")); movement.put("quantity",purchase?quantity:quantity.negate());
            movement.put("balance",nextStock); movement.put("unitCost",purchase?price:average);
            movement.put("businessDate",order.get("business_date")); db.insertMovement(movement);
        }
        db.completeOrder(id);
        try{
            String payload=json.writeValueAsString(detail(id));
            training.enqueue(id,"KINGDEE","SAVE_DOCUMENT",payload);
            training.enqueue(id,"WMS",purchase?"RECEIPT":"ISSUE",payload);
        }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("接口任务创建失败",e);}
        workflow.audit(purchase?"采购入库":"销售出库",order.get("order_no").toString(),"库存与流水已更新，接口任务已创建");
        return detail(id);
    }

    @Transactional
    public Map<String,Object> cancel(long id) {
        var order=existing(db.lockOrder(id),"单据不存在");
        require(order.get("status").equals("DRAFT"),"只有待处理单据可以作废");
        var flow=training.workflow(id);
        require(flow==null||!flow.get("state").equals("SUBMITTED"),"审批中的单据请先由主管驳回");
        db.cancelOrder(id);workflow.audit("作废单据",order.get("order_no").toString(),"未改变库存");return detail(id);
    }

    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Map<String,Object> workspace() {
        return Map.of("products",db.products(),"partners",db.partners(),"orders",db.orders(),"movements",db.movements(),"salesLines",db.salesLines(),
            "workflows",training.workflows(),"jobs",training.jobs(),"audits",training.audits(),
            "today",LocalDate.now(ZoneId.of("Asia/Shanghai")).toString());
    }
}
