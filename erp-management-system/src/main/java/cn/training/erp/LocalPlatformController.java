package cn.training.erp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/training-api")
public class LocalPlatformController {
    private final LocalPlatformService service;
    private final TrainingMapper db;
    @Value("${erp.integrations.token}") private String token;
    public LocalPlatformController(LocalPlatformService service,TrainingMapper db){this.service=service;this.db=db;}
    private void authorize(String supplied){
        if(!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"接口令牌无效");
    }
    @GetMapping("/health") public Map<String,Object> health(@RequestHeader(value="X-Training-Token",defaultValue="")String supplied){
        authorize(supplied);return Map.of("status","UP","environment","LOCAL_TRAINING","targets",List.of("KINGDEE","WMS","MOM"));
    }
    @GetMapping("/kingdee/materials") public Map<String,Object> materials(@RequestHeader(value="X-Training-Token",defaultValue="")String supplied){
        authorize(supplied);return Map.of("success",true,"data",service.materials());
    }
    @PostMapping("/{target:kingdee|wms}/documents") public Map<String,Object> receive(
        @PathVariable String target,@RequestHeader(value="X-Training-Token",defaultValue="")String supplied,
        @RequestHeader(value="Idempotency-Key",defaultValue="")String key,@RequestBody Map<String,Object> body){
        authorize(supplied);return service.receive(target.toUpperCase(Locale.ROOT),key,body);
    }
    @GetMapping("/{target:kingdee|wms}/documents") public List<Map<String,Object>> receipts(
        @PathVariable String target,@RequestHeader(value="X-Training-Token",defaultValue="")String supplied){
        authorize(supplied);return db.receipts(target.toUpperCase(Locale.ROOT));
    }
    @PostMapping(value="/mom",consumes={"text/xml","application/soap+xml"},produces="text/xml;charset=UTF-8")
    public String mom(@RequestHeader(value="X-Training-Token",defaultValue="")String supplied,@RequestBody String xml){
        authorize(supplied);return service.soap(xml);
    }
    @PostMapping("/control/{target}/fail-next") public Map<String,Object> fail(
        @PathVariable String target,@RequestHeader(value="X-Training-Token",defaultValue="")String supplied){
        authorize(supplied);service.failNext(target);return Map.of("configured",true,"target",target);
    }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String,String>> temporary(IllegalStateException e){
        return ResponseEntity.status(503).body(Map.of("message",e.getMessage()));
    }
}
