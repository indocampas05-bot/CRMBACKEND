// src/main/java/com/indocrm/indocrm/config/BridgeKeyFilter.java
//
// NEW FILE. Lets the Windows companion (which has no JWT) call the local-SIM
// device-bridge endpoints by sending   X-Bridge-Key: <bridge.api-key>
//
//   GET  /api/local-sim/calls/bridge/next?deviceId=...&agentUsername=...
//   POST /api/local-sim/calls/{requestId}/status
//
// Add to application.properties (use a long random value, same one goes into
// the companion's config.json as "bridgeKey"):
//   bridge.api-key=PUT-A-LONG-RANDOM-SECRET-HERE

package com.indocrm.indocrm.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BridgeKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Bridge-Key";
    private static final String PREFIX = "/api/local-sim/calls/";

    @Value("${bridge.api-key:}")
    private String bridgeApiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith(PREFIX)) return true;

        // companion polling endpoint
        if (path.startsWith(PREFIX + "bridge/")) return false;

        // companion status report
        return !("POST".equalsIgnoreCase(request.getMethod()) && path.endsWith("/status"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String provided = request.getHeader(HEADER);

        // No bridge key -> not the companion. Let JWT / Spring Security decide
        // (a logged-in CRM user may legitimately POST a status too).
        if (provided == null || provided.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        if (bridgeApiKey == null || bridgeApiKey.isBlank()) {
            reject(response, "bridge.api-key is not configured on the server");
            return;
        }

        boolean ok = MessageDigest.isEqual(
                provided.trim().getBytes(StandardCharsets.UTF_8),
                bridgeApiKey.trim().getBytes(StandardCharsets.UTF_8));

        if (!ok) {
            reject(response, "Invalid bridge key");
            return;
        }

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "windows-companion", null,
                        List.of(new SimpleGrantedAuthority("ROLE_BRIDGE")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}