package com.indocrm.indocrm.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.indocrm.indocrm.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final WebSocketSecurityFilter webSocketSecurityFilter;
    private final BridgeKeyFilter bridgeKeyFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            UserDetailsService userDetailsService,
            WebSocketSecurityFilter webSocketSecurityFilter,
            BridgeKeyFilter bridgeKeyFilter) {

        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.webSocketSecurityFilter = webSocketSecurityFilter;
        this.bridgeKeyFilter = bridgeKeyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http

            /*
             * WebSocket security filter
             */
            .addFilterBefore(
                webSocketSecurityFilter,
                UsernamePasswordAuthenticationFilter.class
            )

            /*
             * CSRF disabled because this application
             * uses JWT authentication.
             */
            .csrf(csrf -> csrf.disable())

            /*
             * CORS - THIS IS THE CRITICAL FIX
             */
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            /*
             * Authorization
             */
            .authorizeHttpRequests(auth -> auth

                /*
                 * ==========================================
                 * PUBLIC APIs
                 * ==========================================
                 */
                // Registration is NOT public any more (it let anyone create an account
                // that could then read every "authenticated()" endpoint).
                .requestMatchers("/api/auth/register")
                .hasAnyRole("SUPER_ADMIN", "ADMIN")

                .requestMatchers(
                    "/api/auth/**",
                    "/api/public/**",
                    "/api/google-meet/oauth/callback",
                    "/error"
                )
                .permitAll()

                /*
                 * ==========================================
                 * WEBSOCKET / BRIDGE
                 * ==========================================
                 */
                .requestMatchers(
                    "/ws/**",
                    "/bridge/**",
                    "/ws",
                    "/bridge"
                )
                .permitAll()

                .requestMatchers("/app/**")
                .permitAll()

                .requestMatchers("/topic/**")
                .permitAll()

                .requestMatchers("/user/**")
                .permitAll()

                /*
                 * ==========================================
                 * LOCAL-SIM DEVICE BRIDGE (Windows companion)
                 * ==========================================
                 * The companion has no JWT. It authenticates with the
                 * X-Bridge-Key header (see BridgeKeyFilter), which grants
                 * ROLE_BRIDGE. Only the polling endpoint is restricted to it;
                 * POST /{id}/status accepts BRIDGE or a normal logged-in user.
                 */
                .requestMatchers("/api/local-sim/calls/bridge/**")
                .hasRole("BRIDGE")

                /*
                 * ==========================================
                 * SUPER ADMIN APIs
                 * ==========================================
                 */
                .requestMatchers("/api/super-admin/**")
                .hasAnyRole(
                    "SUPER_ADMIN",
                    "ADMIN"
                )

                /*
                 * ==========================================
                 * LEAVE PERMISSION ADMIN APIs
                 * ==========================================
                 */
                .requestMatchers(
                    "/api/leave-permissions/admin/**"
                )
                .hasAnyRole(
                    "SUPER_ADMIN",
                    "ADMIN"
                )

                .requestMatchers(
                    "/api/leave-permissions/*/status"
                )
                .hasAnyRole(
                    "SUPER_ADMIN",
                    "ADMIN"
                )

                /*
                 * ==========================================
                 * QUOTATION ADMIN APIs
                 * ==========================================
                 */
                .requestMatchers(
                    "/api/quotations/admin/**"
                )
                .hasAnyRole(
                    "SUPER_ADMIN",
                    "ADMIN"
                )

                .requestMatchers(
                    "/api/quotations/*/review"
                )
                .hasAnyRole(
                    "SUPER_ADMIN",
                    "ADMIN"
                )

                /*
                 * ==========================================
                 * ALL OTHER APIs
                 * ==========================================
                 */
                .anyRequest()
                .authenticated()
            )

            /*
             * ==========================================
             * SESSION MANAGEMENT
             * ==========================================
             */
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            /*
             * Not logged in / expired token  -> 401  (frontends then go to the login page)
             * Logged in but wrong role       -> 403  (frontends show "no permission")
             * Spring's default answered 403 for BOTH, so an expired token looked like a
             * permission problem and the apps behaved randomly.
             */
            .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                res.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.getWriter().write("{\"message\":\"Authentication required\",\"error\":\"Authentication required\"}");
            }))

            /*
             * ==========================================
             * AUTHENTICATION PROVIDER
             * ==========================================
             */
            .authenticationProvider(
                authenticationProvider()
            )

            /*
             * ==========================================
             * JWT FILTER
             * ==========================================
             */
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            )

            /*
             * Bridge-key filter for the Windows companion (no JWT)
             */
            .addFilterBefore(
                bridgeKeyFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /*
     * ==========================================
     * CORS CONFIGURATION - SINGLE SOURCE OF TRUTH
     * ==========================================
     */
   @Bean
public CorsConfigurationSource corsConfigurationSource() {

    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOriginPatterns(
        Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://192.168.0.*:*",
            "http://192.168.1.*:*",
            "http://192.168.*.*:*",

            "https://thespian-impulse-glorious.ngrok-free.dev",
            "https://*.ngrok-free.dev",
            "https://*.trycloudflare.com",
            "https://*.cfargotunnel.com"
        )
    );

    configuration.setAllowedMethods(
        Arrays.asList(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "PATCH",
            "OPTIONS"
        )
    );

    configuration.setAllowedHeaders(
        Arrays.asList(
            "*"
        )
    );

    configuration.setExposedHeaders(
        Arrays.asList(
            "Authorization",
            "Content-Disposition"
        )
    );

    configuration.setAllowCredentials(true);

    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source =
        new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration(
        "/**",
        configuration
    );

    return source;
}
    /*
     * ==========================================
     * AUTHENTICATION PROVIDER
     * ==========================================
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider authProvider =
            new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(
            userDetailsService
        );

        authProvider.setPasswordEncoder(
            passwordEncoder()
        );

        return authProvider;
    }

    /*
     * ==========================================
     * AUTHENTICATION MANAGER
     * ==========================================
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config)
            throws Exception {

        return config.getAuthenticationManager();
    }

    /*
     * ==========================================
     * PASSWORD ENCODER
     * ==========================================
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}


