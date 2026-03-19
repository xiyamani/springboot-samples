package top.xiyamani.springbootweek03.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Slf4j
class AppConfigTest {
    @Resource
    private AppConfig appConfig;

    @Test
    void getAppName() {
        log.info("应用名称:{}", appConfig.getAppName());
    }

    @Test
    void getVersion() {
        log.info("版本号：{}", appConfig.getVersion());
    }

    @Test
    void getDescription() {
        log.info("应用描述{}", appConfig.getDescription());
    }
}