package cn.training.erp;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.boot.availability.*;
import static cn.training.erp.Models.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final ErpService service;
    private final ErpMapper db;
    private final ApplicationAvailability availability;
    public ApiController(ErpService service,ErpMapper db,ApplicationAvailability availability) { this.service=service; this.db=db;this.availability=availability; }

    @GetMapping("/health") public ResponseEntity<Map<String,String>> health() {
        boolean ready=availability.getReadinessState()==ReadinessState.ACCEPTING_TRAFFIC;
        return ResponseEntity.status(ready?200:503).body(Map.of("status",ready?"UP":"STARTING","version","2.0.0","application","hengxu-erp"));
    }
    @GetMapping("/workspace") public Map<String,Object> workspace() { return service.workspace(); }
    @PostMapping("/products") public Map<String,Object> product(@Valid @RequestBody ProductInput body) { return service.saveProduct(null,body); }
    @PutMapping("/products/{id}") public Map<String,Object> product(@PathVariable long id,@Valid @RequestBody ProductInput body) { return service.saveProduct(id,body); }
    @PostMapping("/partners") public Map<String,Object> partner(@Valid @RequestBody PartnerInput body) { return service.addPartner(body); }
    @PostMapping("/orders") public Map<String,Object> order(@Valid @RequestBody OrderInput body) { return service.createOrder(body); }
    @GetMapping("/orders/{id}") public Map<String,Object> order(@PathVariable long id) { return service.detail(id); }
    @PostMapping("/orders/{id}/post") public Map<String,Object> post(@PathVariable long id) { return service.post(id); }
    @PostMapping("/orders/{id}/cancel") public Map<String,Object> cancel(@PathVariable long id) { return service.cancel(id); }

    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> export(@PathVariable String type) {
        StringBuilder csv=new StringBuilder("\uFEFF");
        switch(type) {
            case "inventory" -> {
                csv.append("商品编码,商品名称,分类,单位,库存数量,预警下限,平均成本,库存金额\r\n");
                for(var p:db.products()) csv.append(row(p.get("sku"),p.get("name"),p.get("category"),p.get("unit"),p.get("stock"),p.get("min_stock"),p.get("average_cost"),
                    ErpService.money(ErpService.decimal(p,"stock").multiply(ErpService.decimal(p,"average_cost")))));
            }
            case "orders" -> {
                csv.append("单据编号,单据类型,往来单位,业务日期,状态,总金额,备注\r\n");
                for(var o:db.orders()) csv.append(row(o.get("order_no"),o.get("type").equals("PURCHASE")?"采购":"销售",o.get("partner_name"),o.get("business_date"),
                    switch(o.get("status").toString()) { case "DRAFT"->"待处理";case "COMPLETED"->"已完成";default->"已作废";},o.get("total_amount"),o.get("note")));
            }
            case "movements" -> {
                csv.append("流水编号,商品编码,商品名称,类型,变动数量,结存数量,单位成本,业务日期,来源单据\r\n");
                for(var m:db.movements()) csv.append(row(m.get("id"),m.get("sku"),m.get("product_name"),
                    switch(m.get("type").toString()) {case "PURCHASE"->"采购入库";case "SALE"->"销售出库";default->"期初库存";},
                    m.get("quantity"),m.get("balance"),m.get("unit_cost"),m.get("business_date"),m.get("order_no")));
            }
            default -> throw new IllegalArgumentException("不支持的导出类型");
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"erp-"+type+".csv\"")
            .contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    private static String row(Object... values) {
        List<String> cells=new ArrayList<>();
        for(Object value:values) {
            String s=value==null?"":value.toString();
            String trimmed=s.stripLeading();
            if (!(value instanceof Number) && !trimmed.isEmpty() && "=+-@\t\r".indexOf(trimmed.charAt(0))>=0) s="'"+s;
            cells.add("\""+s.replace("\"","\"\"")+"\"");
        }
        return String.join(",",cells)+"\r\n";
    }

    @RestControllerAdvice
    public static class Errors {
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));
        }
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException e) {
            String message=e.getBindingResult().getFieldErrors().stream()
                .map(f->f.getField()+": "+f.getDefaultMessage()).findFirst().orElse("请检查输入内容");
            return ResponseEntity.badRequest().body(Map.of("message",message));
        }
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<Map<String,String>> malformed() {
            return ResponseEntity.badRequest().body(Map.of("message","请求格式不正确，请检查日期、数量和金额"));
        }
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<Map<String,String>> duplicate() {
            return ResponseEntity.status(409).body(Map.of("message","数据冲突：请检查商品编码、单位名称是否重复，以及数值是否超过范围"));
        }
    }
}
