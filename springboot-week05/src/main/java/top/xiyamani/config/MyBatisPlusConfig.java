package top.xiyamani.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * MyBatis-Plus 配置类
 *
 * @author mqxu
 * @date 2026/4/2
 */
@Configuration
public class MyBatisPlusConfig {
    /**
     * 配置 MyBatis-Plus 拦截器
     * 添加分⻚插件，指定数据库类型为 MySQL
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分⻚插件
        // DbType.MYSQL：指定数据库类型为 MySQL，⽣成的分⻚ SQL 会使⽤ LIMIT
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置请求的⻚⾯⼤于最⼤⻚后的操作：true 返回⾸⻚，false 继续请求（默认 false）
        paginationInterceptor.setOverflow(false);
        // 设置单⻚分⻚条数限制（默认 500 条，-1 不限制）
        paginationInterceptor.setMaxLimit(500L);
        // 添加拦截器
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}
