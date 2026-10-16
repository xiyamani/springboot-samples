package cn.training.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static cn.training.erp.Models.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:erp_tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","erp.workflow.required=false","erp.integrations.auto-dispatch=false"
})
@AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(username="admin",roles="ADMIN")
class BusinessFlowTest {
    @Autowired ErpService service;
    @Autowired ErpMapper db;
    @Autowired MockMvc mvc;
    static final LocalDate TODAY=LocalDate.now(ZoneId.of("Asia/Shanghai"));
    static BigDecimal n(String value){return new BigDecimal(value);}
    long product(){
        var p=service.saveProduct(null,new ProductInput("T-"+UUID.randomUUID().toString().substring(0,12),"测试商品","测试","件",n("10"),n("20"),n("2")));
        return ErpService.number(p,"id");
    }
    long partner(String type){
        return ErpService.number(service.addPartner(new PartnerInput("测试单位"+UUID.randomUUID(),type,"","")),"id");
    }
    long order(String type,LineInput... lines){
        return ErpService.number(service.createOrder(new OrderInput(type,partner(type.equals("SALE")?"CUSTOMER":"SUPPLIER"),TODAY,"测试",List.of(lines))),"id");
    }
    long purchase(long product,String quantity,String price){
        long id=order("PURCHASE",new LineInput(product,n(quantity),n(price)));service.post(id);return id;
    }
    BigDecimal stock(long id){return ErpService.decimal(db.product(id),"stock");}
    void equal(String expected,BigDecimal actual){assertEquals(0,n(expected).compareTo(actual));}

    @Test void purchaseAndSaleUseWeightedCostAndRecordLedger(){
        long p=product();purchase(p,"10","10");purchase(p,"10","20");
        equal("20",stock(p));equal("15",ErpService.decimal(db.product(p),"average_cost"));
        long sale=order("SALE",new LineInput(p,n("4"),n("25")));service.post(sale);
        equal("16",stock(p));equal("60",ErpService.decimal(db.lines(sale).getFirst(),"cost_amount"));
        assertEquals("COMPLETED",db.order(sale).get("status"));
        assertEquals(3,db.movements().stream().filter(m->ErpService.number(m,"product_id")==p).count());
    }
    @Test void insufficientStockLeavesOrderAndInventoryUntouched(){
        long p=product();purchase(p,"2","10");
        long sale=order("SALE",new LineInput(p,n("3"),n("20")));
        assertThrows(IllegalArgumentException.class,()->service.post(sale));
        equal("2",stock(p));assertEquals("DRAFT",db.order(sale).get("status"));
        assertTrue(db.movements().stream().noneMatch(m->Objects.equals(m.get("order_id"),sale)));
    }
    @Test void secondPostingCannotDuplicateStock(){
        long p=product();long id=purchase(p,"5","10");
        assertThrows(IllegalArgumentException.class,()->service.post(id));equal("5",stock(p));
    }
    @Test void multiLineFailureRollsBackEarlierStockAndLedger(){
        long p1=product(),p2=product();purchase(p1,"10","10");
        long sale=order("SALE",new LineInput(p1,n("5"),n("20")),new LineInput(p2,n("1"),n("20")));
        assertThrows(IllegalArgumentException.class,()->service.post(sale));
        equal("10",stock(p1));equal("0",stock(p2));
        assertEquals("DRAFT",db.order(sale).get("status"));
        assertTrue(db.movements().stream().noneMatch(m->Objects.equals(m.get("order_id"),sale)));
        equal("0",ErpService.decimal(db.lines(sale).getFirst(),"cost_amount"));
    }
    @Test void cancelledOrderCannotBePosted(){
        long p=product();long id=order("PURCHASE",new LineInput(p,n("5"),n("10")));
        service.cancel(id);assertThrows(IllegalArgumentException.class,()->service.post(id));equal("0",stock(p));
    }
    @Test void rejectsWrongPartnerFutureDateAndDuplicateProducts(){
        long p=product(),supplier=partner("SUPPLIER");
        var line=new LineInput(p,n("1"),n("10"));
        assertThrows(IllegalArgumentException.class,()->service.createOrder(new OrderInput("SALE",supplier,TODAY,"",List.of(line))));
        assertThrows(IllegalArgumentException.class,()->service.createOrder(new OrderInput("PURCHASE",supplier,TODAY.plusDays(1),"",List.of(line))));
        assertThrows(IllegalArgumentException.class,()->service.createOrder(new OrderInput("PURCHASE",supplier,TODAY,"",List.of(line,line))));
    }
    @Test void concurrentSalesCannotOversell() throws Exception {
        long p=product();purchase(p,"10","10");
        long a=order("SALE",new LineInput(p,n("7"),n("20")));
        long b=order("SALE",new LineInput(p,n("7"),n("20")));
        CountDownLatch start=new CountDownLatch(1);
        try(ExecutorService executor=Executors.newFixedThreadPool(2)){
            List<Future<Boolean>> results=new ArrayList<>();
            for(long id:List.of(a,b)) results.add(executor.submit(()->{start.await();try{service.post(id);return true;}catch(IllegalArgumentException e){return false;}}));
            start.countDown();int posted=0;for(var result:results)if(result.get(10,TimeUnit.SECONDS))posted++;
            assertEquals(1,posted);equal("3",stock(p));
        }
    }
    @Test void invalidQuantityRejectedAtHttpBoundary() throws Exception {
        long p=product(),supplier=partner("SUPPLIER");
        String body="{\"type\":\"PURCHASE\",\"partnerId\":"+supplier+",\"businessDate\":\""+TODAY+"\",\"note\":\"\",\"lines\":[{\"productId\":"+p+",\"quantity\":0,\"unitPrice\":10}]}";
        mvc.perform(post("/api/orders").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
    }
    @Test void rejectsCrossOriginWriteRequests() throws Exception {
        mvc.perform(post("/api/orders/1/post").header("Origin","https://external.invalid")).andExpect(status().isForbidden());
    }
    @Test void exportEscapesSpreadsheetFormulas() throws Exception {
        service.saveProduct(null,new ProductInput("CSV-"+UUID.randomUUID().toString().substring(0,8),"=1+1","测试","件",n("1"),n("2"),n("0")));
        mvc.perform(get("/api/export/inventory")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("\"'=1+1\"")));
    }
}
