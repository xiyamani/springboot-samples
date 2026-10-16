package cn.training.erp;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
public class TrainingController {
    private final WorkflowService workflow;
    private final IntegrationService integrations;
    private final KingdeeApiClient kingdee;
    public TrainingController(WorkflowService workflow,IntegrationService integrations,KingdeeApiClient kingdee){this.workflow=workflow;this.integrations=integrations;this.kingdee=kingdee;}
    @GetMapping("/auth/session") public Map<String,Object> session(Authentication auth,CsrfToken csrf){
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("csrfToken",csrf.getToken());
        result.put("user",auth==null||auth instanceof AnonymousAuthenticationToken?null:Map.of("username",auth.getName(),"roles",auth.getAuthorities().stream().map(a->a.getAuthority().replace("ROLE_","")).toList()));
        return result;
    }
    @PostMapping("/orders/{id}/submit") public Map<String,Object> submit(@PathVariable long id){return workflow.submit(id);}
    @PostMapping("/orders/{id}/approve") public Map<String,Object> approve(@PathVariable long id,@RequestBody(required=false)Map<String,String> body){return workflow.review(id,true,body==null?"":body.getOrDefault("note",""));}
    @PostMapping("/orders/{id}/reject") public Map<String,Object> reject(@PathVariable long id,@RequestBody Map<String,String> body){return workflow.review(id,false,body.getOrDefault("note",""));}
    @GetMapping("/integrations") public Map<String,Object> status(){return integrations.status();}
    @GetMapping("/integrations/kingdee/demo") public Map<String,Object> kingdeeDemo(){return kingdee.demo();}
    @PostMapping("/integrations/materials/pull") public Map<String,Object> materials(){return integrations.pullMaterials();}
    @PostMapping("/integrations/dispatch") public List<Map<String,Object>> dispatch(){return integrations.runQueue();}
    @PostMapping("/integrations/jobs/{id}/retry") public Map<String,Object> retry(@PathVariable long id){return integrations.retry(id);}
    @GetMapping("/integrations/jobs/{id}") public Map<String,Object> messages(@PathVariable long id){return integrations.messages(id);}
    @GetMapping("/integrations/mom/work-orders") public List<Map<String,Object>> manufacturing(){return integrations.manufacturing();}
    public record ReportInput(String orderNo,BigDecimal quantity,String requestId){}
    @PostMapping("/integrations/mom/report") public Map<String,Object> report(@RequestBody ReportInput body){return integrations.reportWork(body.orderNo(),body.quantity(),body.requestId());}
    @PostMapping("/integrations/{target}/fail-next") public Map<String,String> fail(@PathVariable String target){integrations.failNext(target);return Map.of("target",target);}
}
