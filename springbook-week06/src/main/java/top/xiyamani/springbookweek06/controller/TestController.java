package top.xiyamani.springbookweek06.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据库连接测试控制器
 * 用于验证项目实际连接的数据库
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    // 1. 注入 JdbcTemplate（构造器注入，无警告）
    private final JdbcTemplate jdbcTemplate;
    public TestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询当前项目连接的数据库名
     * 访问地址：http://localhost:8088/api/test/db-name
     */
    @GetMapping("/db-name")
    public String getDbName() {
        // 2. 修复 queryForObject 语法错误，补全参数
        String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        return "当前项目连接的数据库：" + dbName;
    }
}