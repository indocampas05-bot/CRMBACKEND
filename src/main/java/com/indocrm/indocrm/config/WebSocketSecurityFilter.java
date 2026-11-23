package com.indocrm.indocrm.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class WebSocketSecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Allow WebSocket handshake requests without authentication
        // CORS is already handled by SecurityConfig.corsConfigurationSource()
        if (path.startsWith("/ws") || path.startsWith("/bridge") || 
            path.startsWith("/app") || path.startsWith("/topic") || 
            path.startsWith("/user")) {
            
            // Only handle WebSocket-specific headers if needed
            // The main CORS headers are set by SecurityConfig
            
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}