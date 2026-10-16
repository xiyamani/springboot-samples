package cn.training.erp;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import static cn.training.erp.ErpService.*;
import static cn.training.erp.Models.*;

@Service
public class IntegrationService {
    private final TrainingMapper db;
    private final ErpMapper erp;
    private final ErpService service;
    private final WorkflowService workflow;
    private final ObjectMapper json;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    @Value("${erp.integrations.base-url:}") private String baseUrl;
    @Value("${erp.integrations.token}") private String token;
    public IntegrationService(TrainingMapper db,ErpMapper erp,ErpService service,WorkflowService workflow,ObjectMapper json){
        this.db=db;this.erp=erp;this.service=service;this.workflow=workflow;this.json=json;
    }
    @EventListener public void ready(WebServerInitializedEvent event){
        if(baseUrl.isBlank())baseUrl="http://127.0.0.1:"+event.getWebServer().getPort();
    }
    String request(String path,String method,String contentType,String body,String key){
        require(!baseUrl.isBlank(),"接口服务尚未就绪");
        try{
            var builder=HttpRequest.newBuilder(URI.create(baseUrl+"/training-api"+path))
                .timeout(Duration.ofSeconds(5)).header("X-Training-Token",token).header("Content-Type",contentType);
            if(key!=null)builder.header("Idempotency-Key",key);
            var response=client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
            require(response.statusCode()>=200&&response.statusCode()<300,"接口返回 HTTP "+response.statusCode()+"："+response.body().substring(0,Math.min(500,response.body().length())));
            return response.body();
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("接口请求被中断");}
        catch(java.io.IOException e){throw new IllegalStateException("接口暂时无法连接，请稍后重试");}
    }
    public Map<String,Object> status(){
        try{
            request("/health","GET","application/json",null,null);
            return Map.of("connected",true,"environment","业务协同接口","endpoint",baseUrl+"/training-api","jobs",db.jobs(),"exchanges",db.exchanges());
        }catch(RuntimeException e){return Map.of("connected",false,"environment","业务协同接口","endpoint",baseUrl+"/training-api","message",e.getMessage(),"jobs",db.jobs(),"exchanges",db.exchanges());}
    }
    @Transactional public Map<String,Object> pullMaterials(){
        try{
            String response=request("/kingdee/materials","GET","application/json",null,null);
            JsonNode rows=json.readTree(response).path("data");
            require(rows.isArray(),"物料接口格式不正确");
            int added=0;
            Set<String> known=new HashSet<>();for(var p:erp.products())known.add(p.get("sku").toString());
            for(var row:rows){
                if(known.contains(row.path("sku").asText()))continue;
                service.saveProduct(null,new ProductInput(row.path("sku").asText(),row.path("name").asText(),row.path("category").asText(),row.path("unit").asText(),
                    row.path("purchasePrice").decimalValue(),row.path("salePrice").decimalValue(),row.path("minStock").decimalValue()));
                known.add(row.path("sku").asText());added++;
            }
            workflow.audit("同步物料","KINGDEE","物料同步成功");
            db.exchange("KINGDEE","PULL_MATERIALS","GET /kingdee/materials",response,true);
            return Map.of("added",added,"total",rows.size());
        }catch(java.io.IOException e){throw new IllegalArgumentException("物料数据解析失败");}
    }
    @Transactional public List<Map<String,Object>> runQueue(){
        for(var job:db.pendingJobs())deliver(number(job,"id"),false);
        return db.jobs();
    }
    @Transactional public Map<String,Object> retry(long id){
        deliver(id,true);return existing(db.lockJob(id),"接口任务不存在");
    }
    private void deliver(long id,boolean retry){
        var job=existing(db.lockJob(id),"接口任务不存在");
        if(job.get("status").equals("SUCCESS"))return;
        if(!retry&&!job.get("status").equals("PENDING"))return;
        boolean success=false;String response;
        try{
            String target=job.get("target").toString().toLowerCase(Locale.ROOT);
            response=request("/"+target+"/documents","POST","application/json",job.get("payload").toString(),job.get("target")+"-"+job.get("order_id")+"-"+job.get("operation"));
            JsonNode result=json.readTree(response);
            require(result.path("success").asBoolean(false),"接口未确认接收");
            success=true;
        }catch(Exception e){response=e.getMessage()==null?"接口处理失败":e.getMessage();}
        db.updateJob(id,success?"SUCCESS":"FAILED",response,success?"":response.substring(0,Math.min(1000,response.length())));
        db.attempt(id,success,response);
    }
    public Map<String,Object> messages(long id){
        var job=db.jobs().stream().filter(j->number(j,"id")==id).findFirst().orElseThrow(()->new IllegalArgumentException("接口任务不存在"));
        return Map.of("job",job,"attempts",db.attempts(id));
    }
    public List<Map<String,Object>> manufacturing(){
        String request=XmlSupport.envelope("GetWorkOrders",List.of());
        return XmlSupport.records(request("/mom","POST","text/xml",request,null));
    }
    public Map<String,Object> reportWork(String orderNo,java.math.BigDecimal quantity,String key){
        require(orderNo!=null&&!orderNo.isBlank(),"请选择生产工单");
        require(quantity!=null&&quantity.signum()>0&&quantity.scale()<=3,"请填写正确的报工数量");
        require(key!=null&&!key.isBlank()&&key.length()<=100,"报工请求编号不正确");
        String request=XmlSupport.envelope("ReportWork",List.of(Map.of("orderNo",orderNo,"quantity",quantity,"requestId",key)));
        try{
            String response=request("/mom","POST","text/xml",request,null);
            var rows=XmlSupport.records(response);
            require(!rows.isEmpty()&&rows.getFirst().get("success").equals("true"),"报工未成功");
            db.exchange("MOM","REPORT_WORK",request,response,true);
            workflow.audit("生产报工",orderNo,"数量 "+quantity+"，请求 "+key);
            return Map.of("request",request,"response",response,"records",rows);
        }catch(RuntimeException e){db.exchange("MOM","REPORT_WORK",request,e.getMessage(),false);throw e;}
    }
    public void failNext(String target){request("/control/"+target+"/fail-next","POST","application/json","{}",null);}
}
