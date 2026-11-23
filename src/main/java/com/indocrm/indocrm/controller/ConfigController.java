// E:/indocrm/src/main/java/com/indocrm/indocrm/controller/ConfigController.java

package com.indocrm.indocrm.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:3000",
    "https://uncheck-ungreased-doily.ngrok-free.dev"
}, allowCredentials = "true")
public class ConfigController {

    @Value("${app.ngrok.url:http://localhost:5000}")
    private String ngrokUrl;

    @Value("${app.websocket.url:ws://localhost:5000/ws}")
    private String websocketUrl;

    @GetMapping("/ngrok-url")
    public Map<String, String> getNgrokUrl() {
        Map<String, String> response = new HashMap<>();
        response.put("url", ngrokUrl);
        response.put("websocket", websocketUrl);
        return response;
    }

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("ngrokUrl", ngrokUrl);
        config.put("websocketUrl", websocketUrl);
        config.put("apiUrl", ngrokUrl + "/api");
        return config;
    }
}
