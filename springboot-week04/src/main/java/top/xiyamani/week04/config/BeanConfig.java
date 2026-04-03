// BeanConfig.java
package top.xiyamani.week04.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.xiyamani.week04.service.UserService;

@Configuration
public class BeanConfig {
    // 若通过@Bean注册UserService，需确保方法名与注入的属性名一致（或指定bean名称）
    @Bean
    public UserService userService() {
        return new UserService();
    }
}
