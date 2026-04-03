package top.xiyamani.week04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "top.xiyamani")
public class SpringbootWeek04Application {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootWeek04Application.class, args);
    }

}
