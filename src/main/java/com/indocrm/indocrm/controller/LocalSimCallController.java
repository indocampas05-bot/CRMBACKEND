package com.indocrm.indocrm.controller;

import com.indocrm.indocrm.service.LocalSimCallService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/local-sim/calls")
public class LocalSimCallController {

    private final LocalSimCallService localSimCallService;

    public LocalSimCallController(LocalSimCallService localSimCallService) {
        this.localSimCallService = localSimCallService;
    }

    

    private static Authentication auth() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.isAuthenticated() && a.getName() != null
                && !"anonymousUser".equals(a.getName())) ? a : null;
    }

    private static boolean hasRole(Authentication a, String role) {
        return a != null && a.getAuthorities().stream().anyMatch(g -> role.equals(g.getAuthority()));
    }

    private static boolean isBridge(Authentication a) { return hasRole(a, "ROLE_BRIDGE"); }

    private static boolean isAdmin(Authentication a) {
        return hasRole(a, "ROLE_ADMIN") || hasRole(a, "ROLE_SUPER_ADMIN");
    }

    /** The CRM user (null for the companion / anonymous). */
    private static String crmUser(Authentication a) {
        return (a == null || isBridge(a)) ? null : a.getName();
    }

    @PostMapping
    public ResponseEntity<?> createCommand(@RequestBody(required = false) Map<String, Object> request) {
        try {
            Authentication a = auth();
            String user = crmUser(a);
            boolean admin = isAdmin(a);

            if (request != null && user != null) {
                request = new HashMap<>(request);
                Object existing = request.get("agentUsername");
                boolean blank = existing == null || String.valueOf(existing).isBlank();
                // A telecaller can only ever place calls on THEIR OWN companion; the
                // username always comes from the login token, never from the browser.
                if (!admin || blank) request.put("agentUsername", user);
            }

            return ResponseEntity.accepted().body(localSimCallService.createCommand(request, user, admin));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            // companion offline / phone not connected
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not create local SIM command."));
        }
    }

    /** GET /api/local-sim/calls/companion-status[?agentUsername=] */
    @GetMapping("/companion-status")
    public ResponseEntity<?> companionStatus(@RequestParam(required = false) String agentUsername) {
        Authentication a = auth();
        String user = crmUser(a);
        String target = (user != null && !isAdmin(a)) ? user
                : (agentUsername != null && !agentUsername.isBlank() ? agentUsername : user);
        return ResponseEntity.ok(localSimCallService.companionStatus(target));
    }

    /** GET /api/local-sim/calls/{requestId} */
    @GetMapping("/{requestId}")
    public ResponseEntity<?> getCommand(@PathVariable String requestId) {
        Map<String, Object> command = localSimCallService.getCommand(requestId);
        if (command.isEmpty() || !mayAccess(command)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Command not found."));
        }
        return ResponseEntity.ok(command);
    }

   
   @GetMapping("/bridge/next")
public ResponseEntity<?> claimNextPendingCommand(
        @RequestParam String deviceId,
        @RequestParam String agentUsername,
        @RequestParam(name = "btConnected", defaultValue = "true") String btConnectedRaw) {
    try {
        boolean btConnected = parseBool(btConnectedRaw, true);
        return ResponseEntity.ok(
                localSimCallService.claimNextPendingCommand(deviceId, agentUsername, btConnected));
    } catch (IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}

private static boolean parseBool(String raw, boolean fallback) {
    if (raw == null || raw.isBlank()) return fallback;
    String v = raw.trim().toLowerCase();
    if ("true".equals(v) || "1".equals(v) || "yes".equals(v)) return true;
    if ("false".equals(v) || "0".equals(v) || "no".equals(v)) return false;
    return fallback;
}

 @PostMapping("/{requestId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String requestId,
            @RequestBody(required = false) Map<String, Object> request) {
        String status = request == null || request.get("status") == null
                ? null : String.valueOf(request.get("status"));
        String message = request == null || request.get("message") == null
                ? "" : String.valueOf(request.get("message"));
        Integer duration = null;
        if (request != null && request.get("duration") instanceof Number) {
            duration = ((Number) request.get("duration")).intValue();
        }

        try {
            Map<String, Object> existing = localSimCallService.getCommand(requestId);
            if (!existing.isEmpty() && !mayAccess(existing)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Command not found."));
            }

            Map<String, Object> updated = localSimCallService.updateStatus(requestId, status, message, duration);
            if (updated.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Command not found."));
            }
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

   
    private boolean mayAccess(Map<String, Object> command) {
        Authentication a = auth();
        if (a == null || isBridge(a) || isAdmin(a)) return true;
        return a.getName().equalsIgnoreCase(String.valueOf(command.get("agentUsername")));
    }
}
