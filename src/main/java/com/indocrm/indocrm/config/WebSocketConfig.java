// src/main/java/com/indocrm/indocrm/config/WebSocketConfig.java

package com.indocrm.indocrm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String[] ALLOWED_ORIGINS = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175",
        "http://127.0.0.1:5173",
        "http://127.0.0.1:5174",
        "http://127.0.0.1:5175",
        "http://192.168.0.128:5173",
        "http://192.168.0.128:5174",
        "http://192.168.0.128:5175",
        "http://192.168.0.101:5173",
        "http://192.168.0.101:5174",
        "http://192.168.0.101:5175",
        "https://thespian-impulse-glorious.ngrok-free.dev",
        "https://*-indocampas.vercel.app"

    };

    @Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
    ts.setPoolSize(1);
    ts.setThreadNamePrefix("ws-heartbeat-");
    ts.initialize();

    config.enableSimpleBroker("/topic", "/queue", "/user")
          .setHeartbeatValue(new long[]{10000, 10000})
          .setTaskScheduler(ts);
    config.setApplicationDestinationPrefixes("/app");
    config.setUserDestinationPrefix("/user");
}

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(ALLOWED_ORIGINS)
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");

        registry.addEndpoint("/ws")
                .setAllowedOrigins(ALLOWED_ORIGINS);
    }
}