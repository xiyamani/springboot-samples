package top.xiyamani.springbootweek03.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author xiyamani
 * @date 2026/3/19
 * @description AppConfig 单例模式 Singleton
 *
 **/
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {
    private String appName;
    private String version;
    private String description;
    private List<String> envs;
    private Author author;
    private Integer tokens;
    private Boolean enable = true;


    //静态类，Author
    @Data
    public static class Author {
        private String name;
        private String website;
        private String email;

    }

}
