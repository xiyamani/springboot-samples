package cn.training.erp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import static cn.training.erp.ErpService.*;

@Service
public class LocalPlatformService {
    private final TrainingMapper db;
    private final ObjectMapper json;
    private final Map<String,AtomicInteger> failures=new ConcurrentHashMap<>();
    public LocalPlatformService(TrainingMapper db,ObjectMapper json){this.db=db;this.json=json;}
    public void failNext(String target){
        require(Set.of("KINGDEE","WMS","MOM").contains(target),"未知接口");
        failures.computeIfAbsent(target,k->new AtomicInteger()).set(1);
    }
    public void checkAvailable(String target){
        AtomicInteger remaining=failures.get(target);
        if(remaining!=null&&remaining.getAndUpdate(value->Math.max(0,value-1))>0)
            throw new IllegalStateException("测试场景：接口临时不可用，请重试");
    }
    public List<Map<String,Object>> materials(){
        checkAvailable("KINGDEE");
        return List.of(
            Map.of("sku","MAT-003","name","T2 紫铜板","category","原材料","unit","张","purchasePrice",92,"salePrice",136,"minStock",12),
            Map.of("sku","ELE-003","name","工业温度传感器","category","电气件","unit","只","purchasePrice",56,"salePrice",88,"minStock",10));
    }
    @Transactional public Map<String,Object> receive(String target,String key,Map<String,Object> payload){
        require(key!=null&&key.length()<=100&&!key.isBlank(),"缺少有效请求编号");
        var old=db.receipt(target+":"+key);
        if(old!=null)return Map.of("success",true,"receiptNo",old.get("receipt_no"),"duplicate",true);
        checkAvailable(target);
        require(payload.get("order_no")!=null&&payload.get("lines") instanceof List<?> list&&!list.isEmpty(),"单据或明细缺失");
        try{
            String number=target+"-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(Locale.ROOT);
            db.receiptInsert(target+":"+key,target,number,json.writeValueAsString(payload));
            return Map.of("success",true,"receiptNo",number,"duplicate",false);
        }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalArgumentException("报文格式错误");}
    }
    @Transactional public String soap(String xml){
        var document=XmlSupport.parse(xml);
        checkAvailable("MOM");
        if(document.getElementsByTagNameNS(XmlSupport.MOM,"GetWorkOrders").getLength()>0)
            return XmlSupport.envelope("GetWorkOrdersResponse",db.manufacturingOrders());
        require(document.getElementsByTagNameNS(XmlSupport.MOM,"ReportWork").getLength()>0,"不支持的制造服务操作");
        String number=XmlSupport.field(document,"orderNo"),key=XmlSupport.field(document,"requestId");
        require(!key.isBlank()&&key.length()<=100,"请提供报工请求编号");
        BigDecimal quantity;
        try{quantity=new BigDecimal(XmlSupport.field(document,"quantity"));}catch(NumberFormatException e){throw new IllegalArgumentException("报工数量格式错误");}
        require(quantity.signum()>0&&quantity.scale()<=3,"报工数量需大于零且最多三位小数");
        var order=existing(db.lockManufacturingOrder(number),"生产工单不存在");
        var previous=db.manufacturingReport(key);
        if(previous!=null){
            require(previous.get("order_no").equals(number)&&decimal(previous,"quantity").compareTo(quantity)==0,"请求编号已用于其他报工");
            return XmlSupport.envelope("ReportWorkResponse",List.of(Map.of("success",true,"requestId",key,"duplicate",true)));
        }
        BigDecimal next=decimal(order,"completed_qty").add(quantity);
        require(next.compareTo(decimal(order,"planned_qty"))<=0,"报工数量超过工单剩余数量");
        db.manufacturingUpdate(number,next,next.compareTo(decimal(order,"planned_qty"))==0?"COMPLETED":"IN_PROGRESS");
        db.manufacturingReportInsert(key,number,quantity);
        return XmlSupport.envelope("ReportWorkResponse",List.of(Map.of("success",true,"requestId",key,"duplicate",false)));
    }
}
