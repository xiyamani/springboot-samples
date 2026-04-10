package top.xiyamani.zhihu.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
/**
 * SpringDoc OpenAPI 配置类
 *
 * @author xiyamani
 * @date 2026/4/2
 */
@Configuration
public class OpenApiConfig {
    /**
     * 配置 OpenAPI 基本信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
// API 信息
                .info(new Info()
                        .title("知乎专栏 API")
                        .description("知乎专栏 API 接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("mqxu")
                                .email("mqxu@gmail.com")
                                .url("https://github.com/mqxu"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                )
// 服务器配置
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("本地开发环境"),
                        new Server()
                                .url("https://api.example.com")
                                .description("生产环境")
                ))
// 全局 Schema 配置
                .components(new io.swagger.v3.oas.models.Components()
                        .addSchemas("PageResult", createPageResultSchema())
                );
    }
    /**
     * 创建分页结果 Schema
     */
    private Schema<?> createPageResultSchema() {
        return new Schema<>()
                .type("object")
                .addProperty("records", new io.swagger.v3.oas.models.media.ArraySchema()
                        .items(new Schema<>().$ref("#/components/schemas/Special")))
                .addProperty("total", new IntegerSchema())
                .addProperty("size", new IntegerSchema())
                .addProperty("current", new IntegerSchema())
                .addProperty("pages", new IntegerSchema());
    }
}
