package top.xiyamani.zhihu.api.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class MyBatisPlusConfig {
    /**
     * 配置 MyBatis-Plus 拦截器
     * 添加分页插件，指定数据库类型为 MySQL
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
// 添加分页插件
// DbType.MYSQL：指定数据库类型为 MySQL，生成的分页 SQL 会使用 LIMIT
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
// 设置请求的页面大于最大页后的操作：true 返回首页，false 继续请求（默认 false）
        paginationInterceptor.setOverflow(false);
// 设置单页分页条数限制（默认 500 条，-1 不限制）
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}
