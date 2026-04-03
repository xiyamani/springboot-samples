package top.xiyamani.week04.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.xiyamani.week04.entity.Student;

@Configuration
public class StudentConfig {
    @Bean
    public Student Student(){
        Student student = new Student();
        student.setName("张三");
        return student;
    }
}
