package com.indocrm.indocrm.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalSimCallService {

    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "DIAL", "ANSWER", "REJECT", "HANGUP", "HOLD", "RESUME", "MUTE", "UNMUTE");

    /** In-call controls: only valid while the call is CONNECTED or ON_HOLD. */
    private static final Set<String> CONTROL_ACTIONS = Set.of("HOLD", "RESUME", "MUTE", "UNMUTE");

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "PENDING", "DISPATCHED", "RINGING", "CONNECTED", "ON_HOLD",
            "COMPLETED", "REJECTED", "FAILED", "BUSY",
            "NO_ANSWER", "CANCELLED");

    private static final Set<String> TERMINAL = Set.of(
            "COMPLETED", "REJECTED", "FAILED", "BUSY", "NO_ANSWER", "CANCELLED");

    private static final long PENDING_TTL_SECONDS = 90;
    private static final long DISPATCHED_TTL_SECONDS = 60;
    private static final long COMPANION_ONLINE_SECONDS = 20;
    private static final long BT_STICKY_SECONDS = 30;
    private static final long FINISHED_RETENTION_SECONDS = 3600;
    private static final long DUPLICATE_WINDOW_SECONDS = 15;

    private final Map<String, Map<String, Object>> commands = new ConcurrentHashMap<>();
    private final Map<String, Companion> companions = new ConcurrentHashMap<>();
    private final ObjectProvider<SimpMessagingTemplate> messagingProvider;

    private static final class Companion {
        volatile String deviceId = "";
        volatile long lastSeenMs = 0;
        volatile boolean btConnected = false;
        volatile long lastBtTrueMs = 0;
    }

    public LocalSimCallService(ObjectProvider<SimpMessagingTemplate> messagingProvider) {
        this.messagingProvider = messagingProvider;
    }

    public Map<String, Object> createCommand(Map<String, Object> request) {
        return createCommand(request, null, true);
    }

    public Map<String, Object> createCommand(Map<String, Object> request, String actingUser, boolean admin) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }

        String action = normalize(request.get("action"));
        if (action.isBlank()) throw new IllegalArgumentException("action is required.");
        if (!ALLOWED_ACTIONS.contains(action)) throw new IllegalArgumentException("Unsupported action: " + action);

        if ("DIAL".equals(action)) return createDial(request);
        return createFollowUp(action, request, actingUser, admin);
    }

    private Map<String, Object> createDial(Map<String, Object> request) {
        String mobile = stringValue(request.get("leadMobile"));
        if (mobile.isBlank()) mobile = stringValue(request.get("phoneNumber"));
        if (mobile.isBlank()) throw new IllegalArgumentException("leadMobile is required for DIAL.");

        String agent = stringValue(request.get("agentUsername"));
        if (agent.isBlank()) throw new IllegalArgumentException("agentUsername is required.");

        long nowMs = System.currentTimeMillis();
        expireStale(nowMs);

        Map<String, Object> health = companionStatus(agent);
        String state = String.valueOf(health.get("state"));
        if ("OFFLINE".equals(state)) {
            throw new IllegalStateException(String.valueOf(health.get("message")));
        }

        synchronized (commands) {
            for (Map<String, Object> c : commands.values()) {
                synchronized (c) {
                    if ("DIAL".equals(read(c, "action"))
                            && agent.equalsIgnoreCase(read(c, "agentUsername"))
                            && mobile.equals(read(c, "leadMobile"))
                            && isActive(read(c, "status"))
                            && nowMs - readLong(c, "createdAtMs") < DUPLICATE_WINDOW_SECONDS * 1000) {
                        return snapshot(c);
                    }
                }
            }

            for (Map<String, Object> c : commands.values()) {
                synchronized (c) {
                    if ("DIAL".equals(read(c, "action"))
                            && agent.equalsIgnoreCase(read(c, "agentUsername"))
                            && isActive(read(c, "status"))) {
                        finish(c, "PENDING".equals(read(c, "status")) ? "CANCELLED" : "COMPLETED",
                                "Replaced by a newer call request.", null);
                    }
                }
            }

            String requestId = UUID.randomUUID().toString();
            Instant now = Instant.ofEpochMilli(nowMs);

            Map<String, Object> cmd = Collections.synchronizedMap(new LinkedHashMap<>());
            cmd.put("requestId", requestId);
            cmd.put("parentRequestId", "");
            cmd.put("action", "DIAL");
            cmd.put("status", "PENDING");
            cmd.put("leadId", stringValue(request.get("leadId")));
            cmd.put("leadName", stringValue(request.get("leadName")));
            cmd.put("leadMobile", mobile);
            cmd.put("agentUsername", agent);
            cmd.put("message", "Call queued. Waiting for your Windows companion to pick it up.");
            cmd.put("createdAt", now.toString());
            cmd.put("updatedAt", now.toString());
            cmd.put("createdAtMs", nowMs);
            cmd.put("updatedAtMs", nowMs);
            commands.put(requestId, cmd);
            return snapshot(cmd);
        }
    }

    private Map<String, Object> createFollowUp(String action, Map<String, Object> request,
                                               String actingUser, boolean admin) {
        String parentId = stringValue(request.get("parentRequestId"));
        if (parentId.isBlank()) parentId = stringValue(request.get("requestId"));
        if (parentId.isBlank()) throw new IllegalArgumentException("parentRequestId is required for " + action + ".");

        Map<String, Object> parent = commands.get(parentId);
        if (parent == null) throw new IllegalArgumentException("Call not found (it may have expired).");

        long nowMs = System.currentTimeMillis();
        expireStale(nowMs);

        synchronized (commands) {
            String owner;
            String parentStatus;
            synchronized (parent) {
                owner = read(parent, "agentUsername");
                parentStatus = read(parent, "status");
            }

            if (!admin && actingUser != null && !actingUser.equalsIgnoreCase(owner)) {
                throw new IllegalArgumentException("Call not found (it may have expired).");
            }

            if (TERMINAL.contains(parentStatus)) return snapshot(parent);

            if (CONTROL_ACTIONS.contains(action)) {
                if (!"CONNECTED".equals(parentStatus) && !"ON_HOLD".equals(parentStatus)) {
                    throw new IllegalArgumentException(
                            action + " is only possible while the call is connected.");
                }
                // Already in the requested state -> nothing to send to the phone.
                if (("HOLD".equals(action) && "ON_HOLD".equals(parentStatus))
                        || ("RESUME".equals(action) && "CONNECTED".equals(parentStatus))) {
                    return snapshot(parent);
                }
            }

            if ("PENDING".equals(parentStatus) && "HANGUP".equals(action)) {
                synchronized (parent) {
                    if ("PENDING".equals(read(parent, "status"))) {
                        finish(parent, "CANCELLED", "Cancelled before dialing.", null);
                    }
                }
                return snapshot(parent);
            }

            for (Map<String, Object> c : commands.values()) {
                synchronized (c) {
                    if (action.equals(read(c, "action"))
                            && parentId.equals(read(c, "parentRequestId"))
                            && isActive(read(c, "status"))) {
                        Map<String, Object> out = snapshot(parent);
                        out.put("followUpRequestId", read(c, "requestId"));
                        return out;
                    }
                }
            }

            String requestId = UUID.randomUUID().toString();
            Instant now = Instant.ofEpochMilli(nowMs);
            Map<String, Object> cmd = Collections.synchronizedMap(new LinkedHashMap<>());
            cmd.put("requestId", requestId);
            cmd.put("parentRequestId", parentId);
            cmd.put("action", action);
            cmd.put("status", "PENDING");
            cmd.put("leadId", read(parent, "leadId"));
            cmd.put("leadName", read(parent, "leadName"));
            cmd.put("leadMobile", read(parent, "leadMobile"));
            cmd.put("agentUsername", owner);
            cmd.put("message", action + " queued for the phone.");
            cmd.put("createdAt", now.toString());
            cmd.put("updatedAt", now.toString());
            cmd.put("createdAtMs", nowMs);
            cmd.put("updatedAtMs", nowMs);
            commands.put(requestId, cmd);

            Map<String, Object> out = snapshot(parent);
            out.put("followUpRequestId", requestId);
            return out;
        }
    }

    public Map<String, Object> getCommand(String requestId) {
        if (requestId == null || requestId.isBlank()) return Collections.emptyMap();
        Map<String, Object> command = commands.get(requestId);
        if (command == null) return Collections.emptyMap();
        expireStale(System.currentTimeMillis());
        return snapshot(command);
    }

    public Map<String, Object> claimNextPendingCommand(String deviceId) {
        throw new IllegalArgumentException("agentUsername is required.");
    }

    public Map<String, Object> claimNextPendingCommand(String deviceId, String agentUsername) {
        return claimNextPendingCommand(deviceId, agentUsername, true);
    }

    public Map<String, Object> claimNextPendingCommand(String deviceId, String agentUsername, boolean btConnected) {
        if (deviceId == null || deviceId.isBlank()) throw new IllegalArgumentException("deviceId is required.");
        String wantedUser = agentUsername == null ? "" : agentUsername.trim();
        if (wantedUser.isEmpty()) throw new IllegalArgumentException("agentUsername is required.");

        long nowMs = System.currentTimeMillis();

        Companion companion = companions.computeIfAbsent(wantedUser.toLowerCase(), k -> new Companion());
        companion.deviceId = deviceId.trim();
        companion.btConnected = btConnected;
        companion.lastSeenMs = nowMs;
        if (btConnected) companion.lastBtTrueMs = nowMs;

        synchronized (commands) {
            expireStale(nowMs);

            Map<String, Object> next = commands.values().stream()
                    .filter(c -> "PENDING".equals(read(c, "status")))
                    .filter(c -> wantedUser.equalsIgnoreCase(read(c, "agentUsername")))
                    .min((l, r) -> {
                        int lp = "HANGUP".equals(read(l, "action")) ? 0 : 1;
                        int rp = "HANGUP".equals(read(r, "action")) ? 0 : 1;
                        if (lp != rp) return Integer.compare(lp, rp);
                        return Long.compare(readLong(l, "createdAtMs"), readLong(r, "createdAtMs"));
                    })
                    .orElse(null);

            if (next == null) return Map.of("available", false);

            synchronized (next) {
                long t = System.currentTimeMillis();
                next.put("status", "DISPATCHED");
                next.put("deviceId", deviceId.trim());
                next.put("message", "Picked up by your Windows companion. Dialing...");
                next.put("updatedAt", Instant.ofEpochMilli(t).toString());
                next.put("updatedAtMs", t);
                Map<String, Object> result = snapshot(next);
                result.put("available", true);
                publish(next);
                return result;
            }
        }
    }

    public Map<String, Object> companionStatus(String agentUsername) {
        String user = agentUsername == null ? "" : agentUsername.trim();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("agentUsername", user);

        Companion c = user.isEmpty() ? null : companions.get(user.toLowerCase());
        long nowMs = System.currentTimeMillis();
        long ageSec = c == null ? Long.MAX_VALUE : (nowMs - c.lastSeenMs) / 1000;
        boolean btLive = c != null && (c.btConnected || (nowMs - c.lastBtTrueMs) < BT_STICKY_SECONDS * 1000);

        if (c == null || ageSec > COMPANION_ONLINE_SECONDS) {
            r.put("state", "OFFLINE");
            r.put("online", false);
            r.put("bluetoothConnected", false);
            r.put("message", "Your Windows companion is offline. Open IndoCRM Companion on your PC and make sure "
                    + "its Telecaller Username is exactly \"" + user + "\".");
            return r;
        }

        r.put("online", true);
        r.put("deviceId", c.deviceId);
        r.put("lastSeenSecondsAgo", ageSec);
        r.put("bluetoothConnected", btLive);

        if (!btLive) {
            r.put("state", "PHONE_NOT_CONNECTED");
            r.put("message", "Companion is online. Windows Bluetooth pairing is not enough — keep the IndoCRM Bridge app open on the phone so the command link can open.");
        } else {
            r.put("state", "READY");
            r.put("message", "Phone bridge is ready.");
        }
        return r;
    }

    public Map<String, Object> updateStatus(String requestId, String status, String message) {
        return updateStatus(requestId, status, message, null);
    }

    public Map<String, Object> updateStatus(String requestId, String status, String message, Integer duration) {
        if (requestId == null || requestId.isBlank()) throw new IllegalArgumentException("requestId is required.");

        String normalizedStatus = normalize(status);
        if (normalizedStatus.isBlank()) throw new IllegalArgumentException("status is required.");
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported status: " + normalizedStatus);
        }

        Map<String, Object> command = commands.get(requestId);
        if (command == null) return Collections.emptyMap();

        String cleanMessage = message == null || message.isBlank() ? "" : message.trim();
        String action;
        String parentId;

        synchronized (command) {
            String current = read(command, "status");

            if (TERMINAL.contains(current)) return snapshot(command);
            // Ignore stale regressions, but allow CONNECTED <-> ON_HOLD transitions.
            if (rank(normalizedStatus) < rank(current)) return snapshot(command);

            if (TERMINAL.contains(normalizedStatus)) {
                finish(command, normalizedStatus, cleanMessage, duration);
            } else {
                long t = System.currentTimeMillis();
                command.put("status", normalizedStatus);
                if (!cleanMessage.isEmpty()) command.put("message", cleanMessage);
                if ("CONNECTED".equals(normalizedStatus)) {
                    if (readLong(command, "connectedAtMs") == 0) command.put("connectedAtMs", t);
                    if (readLong(command, "talkSegmentStartedAtMs") == 0) command.put("talkSegmentStartedAtMs", t);
                } else if ("ON_HOLD".equals(normalizedStatus)) {
                    long segmentStart = readLong(command, "talkSegmentStartedAtMs");
                    if (segmentStart > 0) {
                        command.put("talkAccumulatedMs", readLong(command, "talkAccumulatedMs") + Math.max(0L, t - segmentStart));
                        command.put("talkSegmentStartedAtMs", 0L);
                    }
                }
                command.put("updatedAt", Instant.ofEpochMilli(t).toString());
                command.put("updatedAtMs", t);
                publish(command);
            }
            action = read(command, "action");
            parentId = read(command, "parentRequestId");
        }

        if (CONTROL_ACTIONS.contains(action) && !parentId.isEmpty() && "COMPLETED".equals(normalizedStatus)) {
            applyControlResultToParent(action, parentId);
        }

        if ("HANGUP".equals(action) && !parentId.isEmpty()) {
            Map<String, Object> parent = commands.get(parentId);
            if (parent != null) {
                synchronized (parent) {
                    if (!TERMINAL.contains(read(parent, "status"))) {
                        if ("COMPLETED".equals(normalizedStatus)) {
                            finish(parent, "COMPLETED", "Call ended.", duration);
                        } else if ("FAILED".equals(normalizedStatus)) {
                            parent.put("message", "Could not end the call from the CRM: "
                                    + (cleanMessage.isEmpty() ? "phone did not respond." : cleanMessage)
                                    + " End it on the phone.");
                        }
                    }
                }
            }
        }
        return snapshot(command);
    }

    /**
     * The phone confirmed a HOLD / RESUME / MUTE / UNMUTE. Reflect it on the parent call so the
     * CRM page (which polls the parent call) shows the real state and the talk timer pauses/resumes.
     */
    private void applyControlResultToParent(String action, String parentId) {
        Map<String, Object> parent = commands.get(parentId);
        if (parent == null) return;

        switch (action) {
            case "MUTE":
            case "UNMUTE":
                synchronized (parent) {
                    if (TERMINAL.contains(read(parent, "status"))) return;
                    long t = System.currentTimeMillis();
                    parent.put("muted", "MUTE".equals(action));
                    parent.put("updatedAt", Instant.ofEpochMilli(t).toString());
                    parent.put("updatedAtMs", t);
                }
                break;
            case "HOLD":
                updateStatus(parentId, "ON_HOLD", "Call on hold", null);
                break;
            case "RESUME":
                updateStatus(parentId, "CONNECTED", "Call resumed", null);
                break;
            default:
                break;
        }
    }

    private void finish(Map<String, Object> c, String status, String message, Integer duration) {
        long t = System.currentTimeMillis();
        c.put("status", status);
        if (message != null && !message.isEmpty()) c.put("message", message);
        long segmentStart = readLong(c, "talkSegmentStartedAtMs");
        long accumulated = readLong(c, "talkAccumulatedMs");
        if (segmentStart > 0) accumulated += Math.max(0L, t - segmentStart);
        int secs = (int) (accumulated / 1000L);
        c.put("talkAccumulatedMs", accumulated);
        c.put("talkSegmentStartedAtMs", 0L);
        c.put("duration", secs);
        c.put("updatedAt", Instant.ofEpochMilli(t).toString());
        c.put("updatedAtMs", t);
        publish(c);
    }

    private void expireStale(long nowMs) {
        for (Map<String, Object> c : commands.values()) {
            synchronized (c) {
                String status = read(c, "status");
                long age = (nowMs - readLong(c, "updatedAtMs")) / 1000;
                long createdAge = (nowMs - readLong(c, "createdAtMs")) / 1000;

                if ("PENDING".equals(status) && createdAge > PENDING_TTL_SECONDS) {
                    String agent = read(c, "agentUsername");
                    Companion companion = agent.isEmpty() ? null : companions.get(agent.toLowerCase());
                    boolean online = companion != null
                            && (nowMs - companion.lastSeenMs) / 1000 <= COMPANION_ONLINE_SECONDS;
                    if (online) {
                        continue;
                    }
                    finish(c, "CANCELLED",
                            "Your Windows companion did not pick this up in time. Check that it is running, "
                          + "the IndoCRM Bridge app is open on the phone, and the Telecaller Username matches your CRM login.",
                            null);
                } else if ("DISPATCHED".equals(status) && age > DISPATCHED_TTL_SECONDS) {
                    finish(c, "FAILED", "The phone did not confirm the call. Keep the IndoCRM Bridge app open and wait until Companion shows Bluetooth Connected.", null);
                } else if (TERMINAL.contains(status) && age > FINISHED_RETENTION_SECONDS) {
                    commands.remove(read(c, "requestId"));
                }
            }
        }
    }

    private void publish(Map<String, Object> c) {
        try {
            SimpMessagingTemplate template = messagingProvider.getIfAvailable();
            if (template == null) return;
            String raw = read(c, "status");
            Map<String, Object> msg = new HashMap<>();
            msg.put("requestId", read(c, "requestId"));
            msg.put("parentRequestId", read(c, "parentRequestId"));
            msg.put("callMethod", "local_sim");
            msg.put("status", crmStatus(raw));
            msg.put("localStatus", raw);
            msg.put("leadId", read(c, "leadId"));
            msg.put("agentUsername", read(c, "agentUsername"));
            msg.put("message", read(c, "message"));
            Object d = c.get("duration");
            long accumulated = readLong(c, "talkAccumulatedMs");
            long segmentStart = readLong(c, "talkSegmentStartedAtMs");
            if (segmentStart > 0) accumulated += Math.max(0L, System.currentTimeMillis() - segmentStart);
            msg.put("duration", (int) (accumulated / 1000L));
            msg.put("timestamp", System.currentTimeMillis());
            template.convertAndSend("/topic/call-status", msg);
        } catch (Exception ignored) {
        }
    }

    private static String crmStatus(String s) {
        switch (s) {
            case "PENDING":
            case "DISPATCHED": return "initiating";
            case "RINGING": return "ringing";
            case "CONNECTED": return "connected";
            case "ON_HOLD": return "on_hold";
            case "COMPLETED": return "completed";
            case "NO_ANSWER": return "no_answer";
            case "BUSY": return "busy";
            case "REJECTED": return "rejected";
            default: return "failed";
        }
    }

    private static int rank(String status) {
        switch (status) {
            case "PENDING": return 0;
            case "DISPATCHED": return 1;
            case "RINGING": return 2;
            case "CONNECTED":
            case "ON_HOLD": return 3;
            default: return 4;
        }
    }

    private static boolean isActive(String status) { return !TERMINAL.contains(status); }

    private static String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toUpperCase();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String read(Map<String, Object> map, String key) {
        synchronized (map) {
            Object value = map.get(key);
            return value == null ? "" : String.valueOf(value);
        }
    }

    private static long readLong(Map<String, Object> map, String key) {
        synchronized (map) {
            Object value = map.get(key);
            return value instanceof Number ? ((Number) value).longValue() : 0L;
        }
    }

    private static Map<String, Object> snapshot(Map<String, Object> source) {
        synchronized (source) {
            return new LinkedHashMap<>(source);
        }
    }
}

