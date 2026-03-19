package top.xiyamani.springbootweek03.controller;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.xiyamani.springbootweek03.config.AppConfig;

import java.util.Map;

@RestController
@RequestMapping("/config")
public class BaseConfigController {
    @Value("${server.port}")
    private Integer port;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Value("${spring.application.name}")
    private String applicationname;

    @Value("${app.app-name}")
    private String appName;

    //带默认值
    @Value("${app.version: 1.0.0}")
    private String appversion;

    @Value("${app.description}")
    private String description;

    @Resource
    private AppConfig appConfig;

    @GetMapping("/info")
    public Map<String, Object> getConfigInfo() {
        return  Map.of(
                "port", port,
                "applicationname", applicationname,
                "app-name", appName,
                "appversion", appversion,
                "appdescription",description
        );
    }

    @GetMapping("/info2")
    public Map<String, Object> getConfigInfo2() {
        return  Map.of(
                "appName", appConfig.getAppName(),
                "appVersion", appConfig.getVersion(),
                "appDescription", appConfig.getDescription()
        );
    }
}
