// FILE: src/main/java/com/indocrm/indocrm/service/ExotelService.java
//
// Replaces the old TwilioService / Android-SIM-Bridge call-initiation logic.
//
// HOW EXOTEL CLICK-TO-CALL WORKS (Connect API):
//   1. We call Exotel's /Calls/connect.json with From=agent's mobile, To=customer's mobile
//   2. Exotel FIRST rings the agent's own mobile (the "From" number)
//   3. When the agent picks up, Exotel automatically dials the customer and BRIDGES the two legs
//   4. The customer sees your Exophone (masked virtual number) as caller ID, never the agent's real number
//   5. Exotel posts call progress to our StatusCallback webhook as the call progresses

package com.indocrm.indocrm.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indocrm.indocrm.config.ExotelProperties;

@Service
public class ExotelService {

    private final ExotelProperties props;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ExotelService(ExotelProperties props) {
        this.props = props;
    }

    /**
     * Initiates a click-to-call: rings the telecaller's mobile first, then bridges to the customer.
     *
     * @param agentMobile    Telecaller's own mobile number (Telecaller.mobile), e.g. "9876543210"
     * @param customerMobile Lead/customer's mobile number to call, e.g. "9123456789"
     * @param leadId         Used only to pass through as a custom field in the callback URL, for correlation
     * @return the Exotel Call Sid (Exotel's unique call ID) - store this against your Call record
     */
    public String initiateClickToCall(String agentMobile, String customerMobile, String leadId) {
        String url = props.getApiBaseUrl() + "/v1/Accounts/" + props.getSid() + "/Calls/connect.json";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("From", normalize(agentMobile));
        form.add("To", normalize(customerMobile));
        form.add("CallerId", props.getExophone());
        form.add("CallType", "trans"); // transactional call, no IVR menu
        form.add("TimeLimit", "3600"); // hard cap 60 min per call, tune as needed
        form.add("TimeOut", "30");     // seconds to wait for agent to pick up before giving up
        form.add("Record", props.isRecordCalls() ? "true" : "false");

        if (props.getCallbackBaseUrl() != null && !props.getCallbackBaseUrl().isBlank()) {
            String callback = props.getCallbackBaseUrl() + "/api/exotel/webhook/status?leadId=" + leadId;
            form.add("StatusCallback", callback);
            form.add("StatusCallbackEvents[0]", "terminal"); // call ended (completed/failed/busy/no-answer)
            form.add("StatusCallbackEvents[1]", "answered");  // agent or customer answered
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", basicAuthHeader());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            JsonNode body = objectMapper.readTree(response.getBody());
            // Exotel wraps the call object under "Call"
            JsonNode callNode = body.has("Call") ? body.get("Call") : body;
            return callNode.has("Sid") ? callNode.get("Sid").asText() : null;
        } catch (Exception e) {
            throw new RuntimeException("Exotel call initiation failed: " + e.getMessage(), e);
        }
    }

    private String basicAuthHeader() {
        String creds = props.getApiKey() + ":" + props.getApiToken();
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }

    // Exotel generally expects 10-digit Indian numbers or +91XXXXXXXXXX - strip spaces/dashes
    private String normalize(String number) {
        if (number == null) return null;
        String cleaned = number.replaceAll("[\\s\\-()]", "");
        if (cleaned.startsWith("0") && cleaned.length() == 11) {
            cleaned = cleaned.substring(1);
        }
        return cleaned;
    }
}
