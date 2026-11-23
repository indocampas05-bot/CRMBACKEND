/*package com.indocrm.indocrm.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175",
        "http://127.0.0.1:5173",
        "http://127.0.0.1:5174",
        "http://127.0.0.1:5175",
        "http://192.168.1.180:5173",
        "http://192.168.1.180:5174",
        "http://192.168.1.180:5175",
        "http://192.168.0.116:5173",
        "http://192.168.0.116:5174",
        "http://192.168.0.108:5173",
        "http://192.168.0.108:5174",
        "http://192.168.0.108:5175",
        "http://192.168.0.114:5173",
        "http://192.168.0.114:5174",
        "http://192.168.0.114:5175",
        "https://thespian-impulse-glorious.ngrok-free.dev",
        "https://*.trycloudflare.com",
        "https://*.cfargotunnel.com"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        
        String origin = request.getHeader("Origin");
        
        // Check if origin is allowed
        boolean allowed = false;
        if (origin != null) {
            for (String allowedOrigin : ALLOWED_ORIGINS) {
                if (allowedOrigin.contains("*")) {
                    String pattern = allowedOrigin.replace("*", ".*");
                    if (origin.matches(pattern)) {
                        allowed = true;
                        break;
                    }
                } else if (allowedOrigin.equals(origin)) {
                    allowed = true;
                    break;
                }
            }
        }
        
        // ✅ ALWAYS echo the actual origin, NEVER use "*"
        // This allows credentials (cookies, Authorization headers) to work
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
        
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", 
            "Authorization, Content-Type, Accept, Origin, X-Requested-With, " +
            "Access-Control-Request-Method, Access-Control-Request-Headers, " +
            "Upgrade, Connection, Sec-WebSocket-Key, Sec-WebSocket-Version, " +
            "Sec-WebSocket-Extensions, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Expose-Headers", "Authorization, Content-Disposition");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        chain.doFilter(req, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}
}


package com.indocrm.indocrm.config;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

// @Component   <-- REMOVED: this filter was firing ALONGSIDE SecurityConfig's
//                  CORS config, causing duplicate Access-Control-Allow-Origin
//                  headers on every response -> browser blocks the request.
//                  SecurityConfig.corsConfigurationSource() is now the ONLY
//                  CORS source. Keeping this class (unregistered) in case
//                  it's needed again later.
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}
}*/