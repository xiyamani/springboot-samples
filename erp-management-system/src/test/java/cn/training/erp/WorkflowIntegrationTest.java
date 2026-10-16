package cn.training.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static cn.training.erp.Models.*;
import static cn.training.erp.ErpService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:workflow_tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "erp.workflow.required=true","erp.integrations.auto-dispatch=false"
})
@AutoConfigureMockMvc
class WorkflowIntegrationTest {
    @Autowired ErpService erp;
    @Autowired ErpMapper db;
    @Autowired TrainingMapper training;
    @Autowired WorkflowService workflow;
    @Autowired IntegrationService integrations;
    @Autowired LocalPlatformService local;
    @Autowired SqlLabController sql;
    @Autowired KingdeeApiClient kingdee;
    @Autowired MockMvc mvc;
    long order(){
        long p=number(erp.saveProduct(null,new ProductInput("W-"+UUID.randomUUID().toString().substring(0,10),"流程测试商品","测试","件",new BigDecimal("10"),new BigDecimal("20"),BigDecimal.ONE)),"id");
        long partner=number(erp.addPartner(new PartnerInput("流程测试"+UUID.randomUUID(),"SUPPLIER","","")),"id");
        return number(erp.createOrder(new OrderInput("PURCHASE",partner,LocalDate.now(ZoneId.of("Asia/Shanghai")),"流程测试",List.of(new LineInput(p,new BigDecimal("5"),new BigDecimal("10"))))),"id");
    }
    long approved(){
        long id=order();workflow.submit(id);workflow.review(id,true,"同意");erp.post(id);return id;
    }
    @Test void approvalGatesPostingAndSupportsRejectResubmit(){
        long id=order();
        assertThrows(IllegalArgumentException.class,()->erp.post(id));
        workflow.submit(id);
        assertThrows(IllegalArgumentException.class,()->erp.post(id));
        assertThrows(IllegalArgumentException.class,()->workflow.review(id,false,""));
        workflow.review(id,false,"请复核采购数量");
        assertEquals("REJECTED",training.workflow(id).get("state"));
        workflow.submit(id);workflow.review(id,true,"已复核");erp.post(id);
        assertEquals("COMPLETED",db.order(id).get("status"));
        assertEquals(2,training.jobs().stream().filter(j->number(j,"order_id")==id).count());
    }
    @Test void reviewerCannotCreateOrdersAndAnonymousCannotRead() throws Exception {
        mvc.perform(post("/api/orders").with(user("reviewer").roles("REVIEWER")).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/workspace").with(anonymous())).andExpect(status().isUnauthorized());
    }
    @Test void realHttpFailureRetryAndIdempotentReceipt() throws Exception {
        long id=approved();
        var job=training.jobs().stream().filter(j->number(j,"order_id")==id&&j.get("target").equals("WMS")).findFirst().orElseThrow();
        long jobId=number(job,"id");
        integrations.failNext("WMS");
        assertEquals("FAILED",integrations.retry(jobId).get("status"));
        var succeeded=integrations.retry(jobId);assertEquals("SUCCESS",succeeded.get("status"));assertEquals(2,number(succeeded,"attempts"));
        String key="WMS-"+id+"-RECEIPT";
        var original=training.receipt("WMS:"+key);assertNotNull(original);
        var payload=new com.fasterxml.jackson.databind.ObjectMapper().readValue(job.get("payload").toString(),Map.class);
        assertEquals(true,local.receive("WMS",key,payload).get("duplicate"));
        integrations.retry(jobId);
        assertEquals(2,training.attempts(jobId).size());
    }
    @Test void materialSyncIsRepeatableWithoutResettingStock(){
        var first=integrations.pullMaterials();assertEquals(2,first.get("added"));
        var second=integrations.pullMaterials();assertEquals(0,second.get("added"));
        assertEquals(2,db.products().stream().filter(p->Set.of("MAT-003","ELE-003").contains(p.get("sku"))).count());
    }
    @Test void soapReportsUpdateOnceAndRejectOverReporting(){
        var rows=integrations.manufacturing();assertFalse(rows.isEmpty());
        var row=rows.getFirst();String no=row.get("order_no").toString(),key=UUID.randomUUID().toString();
        BigDecimal before=new BigDecimal(row.get("completed_qty").toString());
        integrations.reportWork(no,new BigDecimal("2"),key);
        integrations.reportWork(no,new BigDecimal("2"),key);
        var after=integrations.manufacturing().stream().filter(w->w.get("order_no").equals(no)).findFirst().orElseThrow();
        assertEquals(0,before.add(new BigDecimal("2")).compareTo(new BigDecimal(after.get("completed_qty").toString())));
        assertThrows(IllegalArgumentException.class,()->integrations.reportWork(no,new BigDecimal("99999"),UUID.randomUUID().toString()));
    }
    @Test void xmlParserRejectsExternalEntityDocuments(){
        assertThrows(IllegalArgumentException.class,()->XmlSupport.parse("<!DOCTYPE x [<!ENTITY a SYSTEM 'file:///invalid'>]><x>&a;</x>"));
    }
    @Test void sqlLabUsesRealPlansAndReturnsSameResults() throws Exception {
        var result=sql.run();
        assertEquals(true,result.get("consistent"));assertEquals(20000L,result.get("tableRows"));
        assertTrue(result.get("afterPlan").toString().toLowerCase(Locale.ROOT).contains("idx_lab_partner_date"));
        assertNotNull(result.get("functionValue"));
    }
    @Test void formLoginCreatesSessionAndLogoutInvalidatesIt() throws Exception {
        var response=mvc.perform(post("/api/auth/login").with(csrf()).param("username","admin").param("password","admin123"))
            .andExpect(status().isOk()).andExpect(authenticated().withUsername("admin")).andReturn();
        var session=(MockHttpSession)response.getRequest().getSession(false);assertNotNull(session);
        mvc.perform(get("/api/workspace").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
        assertTrue(session.isInvalid());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username","admin").param("password","wrong")).andExpect(status().isUnauthorized());
    }
    @Test void writesWithoutCsrfAndPlatformWithoutTokenAreRejected() throws Exception {
        mvc.perform(post("/api/orders").with(user("admin").roles("ADMIN")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/training-api/kingdee/materials")).andExpect(status().isUnauthorized());
    }
    @Test void kingdeeMockAdapterReturnsStandardBillFieldsAndNormalizedRows() {
        var demo=kingdee.demo();
        assertEquals("MOCK",((Map<?,?>)demo.get("status")).get("mode"));
        var purchase=(Map<?,?>)demo.get("purchaseOrders");
        assertEquals("PUR_PurchaseOrder",purchase.get("formId"));
        var row=(Map<?,?>)((List<?>)purchase.get("rows")).getFirst();
        assertNotNull(row.get("FBillNo"));assertNotNull(row.get("FDate"));assertNotNull(row.get("FMaterialNumber"));
        assertEquals(2,kingdee.normalized("PUR_PurchaseOrder").size());
    }
}
