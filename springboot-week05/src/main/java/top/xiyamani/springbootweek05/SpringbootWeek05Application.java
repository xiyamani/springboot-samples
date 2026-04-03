package top.xiyamani.springbootweek05;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "top.xiyamani") // 关键：扫描 top.xiyamani 下所有包
@MapperScan(basePackages = "top.xiyamani.springbootweek05.mapper") // 扫描 Mapper
public class SpringbootWeek05Application {
    public static void main(String[] args) {
        SpringApplication.run(SpringbootWeek05Application.class, args);
    }
}