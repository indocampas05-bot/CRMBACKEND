package com.indocrm.indocrm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            UserDetailsService userDetailsService) {

        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        /*
         * ==========================================
         * LOGIN
         * ==========================================
         */
        if (path.equals("/api/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * ==========================================
         * GET AUTHORIZATION HEADER
         * ==========================================
         */
        final String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null ||
            !authHeader.startsWith("Bearer ")) {

            /*
             * No JWT.
             *
             * Let Spring Security decide whether
             * the endpoint requires authentication.
             */
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * ==========================================
         * EXTRACT JWT
         * ==========================================
         */
        final String jwt =
                authHeader.substring(7);

        try {

            final String username =
                    jwtUtil.extractUsername(jwt);

            /*
             * ==========================================
             * LOAD USER
             * ==========================================
             */
            if (username != null &&
                SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService
                            .loadUserByUsername(username);

                /*
                 * ==========================================
                 * VALIDATE TOKEN
                 * ==========================================
                 */
                if (jwtUtil.validateToken(
                        jwt,
                        userDetails)) {

                    /*
                     * ==========================================
                     * NORMALIZE AUTHORITIES
                     * ==========================================
                     *
                     * Spring's hasRole("ADMIN") expects:
                     *
                     * ROLE_ADMIN
                     *
                     * Spring's hasRole("SUPER_ADMIN") expects:
                     *
                     * ROLE_SUPER_ADMIN
                     */
                    Collection<SimpleGrantedAuthority>
                            authorities =
                            normalizeAuthorities(
                                userDetails
                                    .getAuthorities()
                            );

                    /*
                     * DEBUG LOG
                     */
                    System.out.println(
                        "===================================="
                    );

                    System.out.println(
                        "JWT USER: " + username
                    );

                    System.out.println(
                        "JWT AUTHORITIES: " + authorities
                    );

                    System.out.println(
                        "REQUEST: "
                        + request.getMethod()
                        + " "
                        + request.getRequestURI()
                    );

                    System.out.println(
                        "===================================="
                    );

                    /*
                     * ==========================================
                     * CREATE AUTHENTICATION
                     * ==========================================
                     */
                    UsernamePasswordAuthenticationToken
                            authToken =
                            new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                authorities
                            );

                    authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                            .buildDetails(request)
                    );

                    SecurityContextHolder
                        .getContext()
                        .setAuthentication(authToken);
                }
            }

        } catch (Exception e) {

            /*
             * Invalid JWT should not crash the request.
             */
            System.out.println(
                "JWT authentication failed: "
                + e.getMessage()
            );

            SecurityContextHolder
                .clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /*
     * ==========================================
     * NORMALIZE AUTHORITIES
     * ==========================================
     */
    private Collection<SimpleGrantedAuthority>
    normalizeAuthorities(
            Collection<? extends org.springframework.security.core.GrantedAuthority>
                    existingAuthorities) {

        List<SimpleGrantedAuthority> authorities =
                new ArrayList<>();

        if (existingAuthorities == null) {
            return authorities;
        }

        for (
            org.springframework.security.core.GrantedAuthority authority
            : existingAuthorities
        ) {

            if (authority == null ||
                authority.getAuthority() == null) {

                continue;
            }

            String role =
                    authority.getAuthority().trim();

            if (role.isEmpty()) {
                continue;
            }

            /*
             * Remove existing ROLE_ prefix first.
             */
            if (role.startsWith("ROLE_")) {
                role = role.substring(5);
            }

            /*
             * Normalize common role names.
             */
            role = role.toUpperCase();

            /*
             * Add ROLE_ prefix.
             */
            String normalizedRole =
                    "ROLE_" + role;

            /*
             * Avoid duplicates.
             */
            boolean alreadyExists =
                    authorities.stream()
                        .anyMatch(
                            a -> a.getAuthority()
                                .equals(normalizedRole)
                        );

            if (!alreadyExists) {

                authorities.add(
                    new SimpleGrantedAuthority(
                        normalizedRole
                    )
                );
            }
        }

        return authorities;
    }
}