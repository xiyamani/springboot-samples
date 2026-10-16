package cn.training.erp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * 金蝶云星空 OpenAPI 适配器。
 *
 * MOCK 模式返回按金蝶单据字段组织的教学报文；切换 REAL 后，
 * 同一组业务方法会通过 HTTP 调用配置的金蝶服务地址。课程环境没有企业账套，
 * 因此默认不发送外部请求。
 */
@Service
public class KingdeeApiClient {
    private final ObjectMapper json;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).cookieHandler(cookies).build();
    private volatile boolean authenticated;
    @Value("${erp.kingdee.mode:MOCK}") private String mode;
    @Value("${erp.kingdee.base-url:}") private String baseUrl;
    @Value("${erp.kingdee.app-id:}") private String appId;
    @Value("${erp.kingdee.app-secret:}") private String appSecret;
    @Value("${erp.kingdee.acct-id:}") private String acctId;
    @Value("${erp.kingdee.username:}") private String username;
    @Value("${erp.kingdee.password:}") private String password;
    @Value("${erp.kingdee.lcid:2052}") private String lcid;
    @Value("${erp.kingdee.demo-account-name:金蝶云星辰业务账套}") private String demoAccountName;

    public KingdeeApiClient(ObjectMapper json) {
        this.json = json;
    }

    public Map<String, Object> status() {
        return Map.of(
            "mode", normalizedMode(),
            "displayMode", isMock() ? "标准接口适配" : "实时接口",
            "provider", "金蝶云星空 OpenAPI 适配器",
            "mock", isMock(),
            "accountLabel", isMock() ? demoAccountName : "真实企业账套",
            "baseUrl", baseUrl == null ? "" : baseUrl,
            "credentialsConfigured", !appId.isBlank() || (!acctId.isBlank() && !username.isBlank()),
            "productionSwitch", "erp.kingdee.mode=REAL"
        );
    }

    public Map<String, Object> demo() {
        return Map.of(
            "status", status(),
            "login", login(),
            "purchaseOrders", query("PUR_PurchaseOrder"),
            "salesOrders", query("SAL_SaleOrder"),
            "inventoryDocuments", query("STK_InStock")
        );
    }

    public Map<String, Object> login() {
        if (isMock()) {
            return response(true, "KD-TOKEN-" + lcid, "接口鉴权回执成功，账套："+demoAccountName);
        }
        requireConfig();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("acctID", acctId);
            body.put("username", username);
            body.put("password", password);
            body.put("lcid", Integer.parseInt(lcid));
            body.put("appId", appId);
            body.put("appSecret", appSecret);
            Map<String, Object> result = sendPost("/Kingdee.BOS.WebApi.ServicesStub.AuthService.LoginByAppSecret.common.kdsvc", body, "");
            authenticated = true;
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("金蝶登录请求失败：" + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> query(String formId) {
        if (isMock()) return mockQuery(formId);
        requireConfig();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("formId", formId);
        request.put("fieldKeys", fieldKeys(formId));
        request.put("filterString", "");
        request.put("orderString", "FDate DESC");
        request.put("topRowCount", 100);
        request.put("startRow", 0);
        return post("/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc", request);
    }

    public Map<String, Object> save(String formId, Map<String, Object> model) {
        if (isMock()) {
            return response(true, "MOCK-" + formId + "-" + UUID.randomUUID(), "本地教学回执");
        }
        requireConfig();
        Map<String, Object> request = Map.of(
            "formid", formId,
            "data", model,
            "appid", appId,
            "appsecret", appSecret
        );
        return post("/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Save.common.kdsvc", request);
    }

    public List<Map<String, Object>> normalized(String formId) {
        JsonNode rows = json.valueToTree(query(formId).getOrDefault("rows", List.of()));
        List<Map<String, Object>> result = new ArrayList<>();
        if (rows.isArray()) {
            for (JsonNode row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("billNo", row.path("FBillNo").asText());
                item.put("date", row.path("FDate").asText());
                item.put("materialNumber", row.path("FMaterialNumber").asText());
                item.put("materialName", row.path("FMaterialName").asText());
                item.put("quantity", row.path("FQty").decimalValue());
                item.put("amount", row.path("FPrice").decimalValue().multiply(row.path("FQty").decimalValue()));
                item.put("sourceFormId", formId);
                result.add(item);
            }
        }
        return result;
    }

    private Map<String, Object> mockQuery(String formId) {
        List<Map<String, Object>> rows = switch (formId) {
            case "PUR_PurchaseOrder" -> List.of(
                row("CG-202610-001", "2026-10-14", "MAT-001", "轴承座", "宁波联创机电有限公司", "12", "125.00"),
                row("CG-202610-002", "2026-10-15", "MAT-003", "T2 紫铜板", "苏州华源材料有限公司", "8", "92.00")
            );
            case "SAL_SaleOrder" -> List.of(
                row("XS-202610-001", "2026-10-15", "FIN-001", "精密传动组件 A型", "华东智能装备有限公司", "6", "920.00")
            );
            default -> List.of(
                row("RK-202610-001", "2026-10-15", "MAT-001", "轴承座", "主仓", "12", "125.00")
            );
        };
        return Map.of(
            "success", true,
            "formId", formId,
            "request", Map.of("fieldKeys", fieldKeys(formId), "startRow", 0, "topRowCount", 100),
            "rows", rows,
            "responseStatus", Map.of("IsSuccess", true, "Errors", List.of())
        );
    }

    private Map<String, Object> row(String no, String date, String sku, String name, String partner, String qty, String price) {
        return Map.of(
            "FID", UUID.randomUUID().toString(),
            "FBillNo", no,
            "FDate", date,
            "FPartnerName", partner,
            "FMaterialNumber", sku,
            "FMaterialName", name,
            "FQty", new java.math.BigDecimal(qty),
            "FPrice", new java.math.BigDecimal(price)
        );
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        try {
            if (!authenticated) login();
            return sendPost(path, body, "");
        } catch (Exception ex) {
            throw new IllegalStateException("金蝶接口调用失败：" + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> sendPost(String path, Map<String, Object> body, String token) {
        try {
            String payload = json.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("X-Api-Token", token)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            return json.readValue(response.body(), LinkedHashMap.class);
        } catch (Exception ex) {
            throw new IllegalStateException("金蝶接口调用失败：" + ex.getMessage(), ex);
        }
    }

    private String fieldKeys(String formId) {
        return "FID,FBillNo,FDate,FMaterialID.FNumber,FMaterialID.FName,FQty,FPrice,FPartnerID.FNumber,FPartnerID.FName";
    }

    private boolean isMock() {
        return !"REAL".equalsIgnoreCase(mode);
    }

    private String normalizedMode() {
        return isMock() ? "MOCK" : "REAL";
    }

    private void requireConfig() {
        if (baseUrl.isBlank() || (acctId.isBlank() && appId.isBlank())) {
            throw new IllegalStateException("真实金蝶模式缺少 base-url、acct-id 或 app-id 配置");
        }
    }

    private Map<String, Object> response(boolean success, String token, String message) {
        return Map.of("success", success, "token", token, "message", message);
    }
}
