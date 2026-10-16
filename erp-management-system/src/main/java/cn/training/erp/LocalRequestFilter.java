package cn.training.erp;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

@Component
public class LocalRequestFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
        throws ServletException,IOException {
        response.setHeader("X-Content-Type-Options","nosniff");
        response.setHeader("X-Frame-Options","DENY");
        response.setHeader("Referrer-Policy","same-origin");
        if(request.getRequestURI().startsWith("/api/")) response.setHeader("Cache-Control","no-store");
        if(Set.of("POST","PUT","DELETE","PATCH").contains(request.getMethod())) {
            String origin=request.getHeader("Origin");
            if(origin!=null) {
                boolean valid=false;
                try {
                    URI uri=URI.create(origin);
                    int port=uri.getPort()==-1?(uri.getScheme().equals("https")?443:80):uri.getPort();
                    valid=Set.of("localhost","127.0.0.1").contains(uri.getHost())
                        &&uri.getScheme().equals(request.getScheme())&&port==request.getServerPort();
                } catch(RuntimeException ignored) {}
                if(!valid) {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\":\"不接受外部页面提交的业务操作\"}");
                    return;
                }
            }
        }
        chain.doFilter(request,response);
    }
}
