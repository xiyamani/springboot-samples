package top.xiyamani.week04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import top.xiyamani.week04.config.StudentConfig;
import top.xiyamani.week04.entity.Student;


public class TestStudent {

    public static void main(String[] args) {
        //加载配置类，初始化Spring容器
        ApplicationContext Context = new AnnotationConfigApplicationContext(StudentConfig.class);

        //从容器中获取Bean
        Student student = Context.getBean("student", Student.class);

        //调用方法
        student.study();
        }
    }
