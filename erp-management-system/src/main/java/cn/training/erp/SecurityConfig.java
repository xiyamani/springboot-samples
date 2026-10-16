package cn.training.erp;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean UserDetailsService userDetailsService(PasswordEncoder encoder){
        return new InMemoryUserDetailsManager(
            User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN").build(),
            User.withUsername("buyer").password(encoder.encode("buyer123")).roles("OPERATOR").build(),
            User.withUsername("reviewer").password(encoder.encode("review123")).roles("REVIEWER").build(),
            User.withUsername("keeper").password(encoder.encode("keeper123")).roles("WAREHOUSE").build()
        );
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf->csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/training-api/**"))
            .authorizeHttpRequests(auth->auth
                .requestMatchers("/","/index.html","/app.js","/app.css","/training.css","/vendor/**","/favicon.svg","/api/health","/api/auth/session","/api/auth/login","/training-api/**","/error").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/orders/*/approve","/api/orders/*/reject").hasAnyRole("ADMIN","REVIEWER")
                .requestMatchers(HttpMethod.POST,"/api/orders/*/post").hasAnyRole("ADMIN","WAREHOUSE")
                .requestMatchers(HttpMethod.POST,"/api/orders","/api/orders/*/submit","/api/orders/*/cancel","/api/products","/api/partners").hasAnyRole("ADMIN","OPERATOR")
                .requestMatchers(HttpMethod.PUT,"/api/products/**").hasAnyRole("ADMIN","OPERATOR")
                .requestMatchers(HttpMethod.POST,"/api/integrations/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(login->login.loginProcessingUrl("/api/auth/login")
                .successHandler((request,response,authentication)->{response.setContentType("application/json;charset=UTF-8");response.getWriter().write("{\"success\":true}");})
                .failureHandler((request,response,exception)->{response.setStatus(401);response.setContentType("application/json;charset=UTF-8");response.getWriter().write("{\"message\":\"账号或密码不正确\"}");}))
            .logout(logout->logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID","XSRF-TOKEN")
                .logoutSuccessHandler((request,response,authentication)->{response.setContentType("application/json");response.getWriter().write("{\"success\":true}");}))
            .exceptionHandling(errors->errors
                .authenticationEntryPoint((request,response,exception)->{response.setStatus(401);response.setContentType("application/json;charset=UTF-8");response.getWriter().write("{\"message\":\"请先登录\"}");})
                .accessDeniedHandler((request,response,exception)->{response.setStatus(403);response.setContentType("application/json;charset=UTF-8");response.getWriter().write("{\"message\":\"当前角色无此操作权限，或会话已失效，请重新登录\"}");}));
        return http.build();
    }
}
