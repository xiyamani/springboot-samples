package cn.training.erp;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static cn.training.erp.Models.*;

@Component
public class DemoData implements ApplicationRunner {
    private final ErpMapper db;
    private final ErpService service;
    private final WorkflowService workflow;
    public DemoData(ErpMapper db,ErpService service,WorkflowService workflow) { this.db=db;this.service=service;this.workflow=workflow; }
    private static BigDecimal n(String value) { return new BigDecimal(value); }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        if(db.productCount()>0) return;
        long s1=partner("苏州精工材料有限公司","SUPPLIER","陈经理","13800001001");
        long s2=partner("宁波联创机电有限公司","SUPPLIER","王经理","13800001002");
        long c1=partner("华东智能装备有限公司","CUSTOMER","李经理","13800002001");
        long c2=partner("杭州远航制造有限公司","CUSTOMER","周经理","13800002002");
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        long p1=product("MAT-001","6061 铝合金板","原材料","张","185","245","30","128",today);
        long p2=product("MAT-002","304 不锈钢管","原材料","根","68","95","50","86",today);
        long p3=product("PRT-001","M8 六角螺栓","标准件","个","0.8","1.5","500","2400",today);
        long p4=product("PRT-002","6204 深沟球轴承","标准件","个","16","28","40","32",today);
        long p5=product("ELE-001","24V 开关电源","电气件","台","85","128","15","46",today);
        long p6=product("ELE-002","接近开关 M12","电气件","只","32","52","20","12",today);
        long p7=product("FIN-001","精密传动组件 A型","产成品","套","420","680","10","28",today);
        long p8=product("FIN-002","工业控制箱 B型","产成品","台","680","1080","8","6",today);
        order("PURCHASE",s1,today.minusDays(13),true,new LineInput(p1,n("40"),n("182")),new LineInput(p2,n("60"),n("67")));
        order("SALE",c1,today.minusDays(12),true,new LineInput(p7,n("8"),n("680")),new LineInput(p5,n("12"),n("128")));
        order("PURCHASE",s2,today.minusDays(10),true,new LineInput(p3,n("1000"),n("0.78")),new LineInput(p5,n("20"),n("84")));
        order("SALE",c2,today.minusDays(8),true,new LineInput(p1,n("24"),n("245")),new LineInput(p2,n("35"),n("95")));
        order("SALE",c1,today.minusDays(6),true,new LineInput(p7,n("9"),n("680")),new LineInput(p3,n("600"),n("1.5")));
        order("PURCHASE",s1,today.minusDays(4),true,new LineInput(p1,n("30"),n("184")));
        order("SALE",c2,today.minusDays(2),true,new LineInput(p5,n("18"),n("128")),new LineInput(p2,n("30"),n("95")));
        order("SALE",c1,today,true,new LineInput(p7,n("4"),n("680")),new LineInput(p8,n("2"),n("1080")));
        order("PURCHASE",s2,today,false,new LineInput(p4,n("100"),n("15.5")),new LineInput(p6,n("80"),n("31")));
        order("SALE",c2,today,false,new LineInput(p1,n("20"),n("245")),new LineInput(p5,n("5"),n("128")));
    }
    private long partner(String name,String type,String contact,String phone) {
        return ErpService.number(service.addPartner(new PartnerInput(name,type,contact,phone)),"id");
    }
    private long product(String sku,String name,String category,String unit,String buy,String sell,String min,String stock,LocalDate today) {
        long id=ErpService.number(service.saveProduct(null,new ProductInput(sku,name,category,unit,n(buy),n(sell),n(min))),"id");
        db.updateStock(id,n(stock),n(buy));
        Map<String,Object> m=new HashMap<>();
        m.put("productId",id);m.put("orderId",null);m.put("type","OPENING");m.put("quantity",n(stock));
        m.put("balance",n(stock));m.put("unitCost",n(buy));m.put("businessDate",today.minusDays(30));db.insertMovement(m);
        return id;
    }
    private void order(String type,long partner,LocalDate date,boolean post,LineInput... lines) {
        var order=service.createOrder(new OrderInput(type,partner,date,type.equals("PURCHASE")?"采购业务数据":"销售业务数据",List.of(lines)));
        if(post) {
            long id=ErpService.number(order,"id");
            workflow.submit(id);workflow.review(id,true,"期初业务审核");
            service.post(id);
        }
    }
}
