package com.indocrm.indocrm.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;

import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.client.gridfs.model.GridFSFile;

import com.indocrm.indocrm.model.CommunicationMessage;
import com.indocrm.indocrm.model.Employee;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.model.User;
import com.indocrm.indocrm.repository.CommunicationMessageRepository;
import com.indocrm.indocrm.repository.EmployeeRepository;
import com.indocrm.indocrm.repository.TelecallerRepository;
import com.indocrm.indocrm.repository.UserRepository;

@Service
public class CommunicationService {

    private static final String GROUP_ALL = "GROUP-ALL";

    // Participant cache: 10 s — the poll loop hits this every 5–30 s, so it
    // collapses N unread queries + N user lookups into 1 DB roundtrip.
    private static final long PARTICIPANT_CACHE_TTL_MS = 10_000L;
    private final Map<String, CachedParticipants> participantCache = new HashMap<>();

    private final CommunicationMessageRepository messageRepository;
    private final TelecallerRepository telecallerRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final GridFsTemplate gridFsTemplate;

    public CommunicationService(
            CommunicationMessageRepository messageRepository,
            TelecallerRepository telecallerRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            GridFsTemplate gridFsTemplate) {
        this.messageRepository = messageRepository;
        this.telecallerRepository = telecallerRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.gridFsTemplate = gridFsTemplate;
    }

    private static class CachedParticipants {
        final long at;
        final List<Map<String, Object>> data;
        CachedParticipants(long at, List<Map<String, Object>> data) {
            this.at = at;
            this.data = data;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Auth / roles (unchanged)
    // ═══════════════════════════════════════════════════════════

    private Authentication authentication() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a.getName() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return a;
    }

    public String currentUsername() {
        return authentication().getName();
    }

    private boolean isAdmin() {
        return authentication().getAuthorities().stream().anyMatch(a -> {
            String r = a.getAuthority();
            return "ROLE_ADMIN".equals(r) || "ROLE_SUPER_ADMIN".equals(r);
        });
    }

    private boolean isTelecaller() {
        return authentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_TELECALLER".equals(a.getAuthority()));
    }

    // ═══════════════════════════════════════════════════════════
    // PARTICIPANTS — cached 10 s
    // ═══════════════════════════════════════════════════════════

    public List<Map<String, Object>> getParticipants() {
        String username = currentUsername();
        long now = System.currentTimeMillis();

        synchronized (participantCache) {
            CachedParticipants cp = participantCache.get(username);
            if (cp != null && now - cp.at < PARTICIPANT_CACHE_TTL_MS) {
                return cp.data;
            }
        }

        List<Map<String, Object>> result = buildParticipants(username);

        synchronized (participantCache) {
            participantCache.put(username, new CachedParticipants(now, result));
            // Drop expired entries
            participantCache.entrySet().removeIf(e -> now - e.getValue().at > PARTICIPANT_CACHE_TTL_MS * 5);
        }

        return result;
    }

    private List<Map<String, Object>> buildParticipants(String username) {
        List<Map<String, Object>> result = new ArrayList<>();

        Map<String, Object> group = new HashMap<>();
        group.put("id", GROUP_ALL);
        group.put("username", GROUP_ALL);
        group.put("name", "All Telecallers + Super Admin");
        group.put("role", "GROUP");
        group.put("group", true);
        group.put("chatCategory", "GROUP");
        group.put("unreadCount", unreadGroupCount(username));
        result.add(group);

        if (isAdmin()) {
            for (Telecaller tc : telecallerRepository.findAll()) {
                if (!tc.isEnabled()) continue;
                if (tc.getStatus() != null && !"Active".equalsIgnoreCase(tc.getStatus())) continue;
                if (tc.getUsername() == null || tc.getUsername().isBlank()) continue;

                Map<String, Object> item = new HashMap<>();
                item.put("id", tc.getUsername());
                item.put("username", tc.getUsername());
                item.put("name", tc.getFullName() != null ? tc.getFullName() : tc.getUsername());
                item.put("role", "Telecaller");
                item.put("group", false);
                item.put("unreadCount", unreadDirectCount(tc.getUsername(), username));
                result.add(item);
            }
            return result;
        }

        // TELECALLER: show other telecallers
        for (Telecaller tc : telecallerRepository.findAll()) {
            if (!tc.isEnabled()) continue;
            if (tc.getStatus() != null && !"Active".equalsIgnoreCase(tc.getStatus())) continue;
            if (tc.getUsername() == null || tc.getUsername().isBlank()
                    || username.equals(tc.getUsername())) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("id", tc.getUsername());
            item.put("username", tc.getUsername());
            item.put("name", tc.getFullName() != null ? tc.getFullName() : tc.getUsername());
            item.put("role", "Telecaller");
            item.put("group", false);
            item.put("chatCategory", "TELECALLER");
            item.put("unreadCount", unreadDirectCount(tc.getUsername(), username));
            result.add(item);
        }

        // Admins
        Set<String> added = new HashSet<>();
        for (User user : userRepository.findAll()) {
            if (!user.isEnabled() || !isAdminRole(user.getRole())) continue;
            if (user.getUsername() == null || user.getUsername().isBlank()) continue;
            if (!added.add(user.getUsername())) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getUsername());
            item.put("username", user.getUsername());
            item.put("name", user.getFullName() != null ? user.getFullName() : user.getUsername());
            item.put("role", normalizeRole(user.getRole()));
            item.put("group", false);
            item.put("chatCategory", "SUPER_ADMIN");
            item.put("unreadCount", unreadDirectCount(user.getUsername(), username));
            result.add(item);
        }

        for (Employee employee : employeeRepository.findAll()) {
            if (!employee.isEnabled() || !isAdminRole(employee.getRole())) continue;
            if (employee.getUsername() == null || employee.getUsername().isBlank()) continue;
            if (!added.add(employee.getUsername())) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("id", employee.getUsername());
            item.put("username", employee.getUsername());
            item.put("name", employee.getFullName() != null ? employee.getFullName() : employee.getUsername());
            item.put("role", normalizeRole(employee.getRole()));
            item.put("group", false);
            item.put("chatCategory", "SUPER_ADMIN");
            item.put("unreadCount", unreadDirectCount(employee.getUsername(), username));
            result.add(item);
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // Messages
    // ═══════════════════════════════════════════════════════════

    public List<CommunicationMessage> getMessages(String chatId) {
        if (chatId == null || chatId.isBlank()) chatId = GROUP_ALL;
        String current = currentUsername();

        List<CommunicationMessage> messages;
        if (GROUP_ALL.equals(chatId)) {
            messages = messageRepository.findByRecipientTypeOrderByCreatedAtAsc("GROUP");
        } else {
            validateDirectChatAccess(chatId);
            messages = messageRepository.findDirectConversation(current, chatId);
            messages.sort(Comparator.comparing(CommunicationMessage::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }
        return messages;
    }

    public CommunicationMessage send(String recipientType, String recipientUsername,
                                     String text, String type, String attachmentId,
                                     String attachmentName, String attachmentContentType,
                                     Integer durationSeconds, Long attachmentSizeBytes) {

        String current = currentUsername();
        boolean group = "GROUP".equalsIgnoreCase(recipientType);
        if (type == null || type.isBlank()) type = "CHAT";

        if (group) {
            if ("ANNOUNCEMENT".equalsIgnoreCase(type) && !isAdmin()) {
                throw new AccessDeniedException("Only Super Admin/Admin can send announcements");
            }
        } else {
            recipientType = "DIRECT";
            if (recipientUsername == null || recipientUsername.isBlank()) {
                throw new IllegalArgumentException("Recipient is required");
            }
            validateDirectChatAccess(recipientUsername);
        }

        boolean hasText = text != null && !text.trim().isEmpty();
        boolean hasAttachment = attachmentId != null && !attachmentId.isBlank();
        if (!hasText && !hasAttachment) {
            throw new IllegalArgumentException("Message or attachment is required");
        }

        CommunicationMessage m = new CommunicationMessage();
        m.setSenderUsername(current);
        m.setSenderName(getDisplayName(current));
        m.setSenderRole(getCurrentRole());
        m.setRecipientType(group ? "GROUP" : "DIRECT");
        m.setRecipientUsername(group ? null : recipientUsername);
        m.setType(type.toUpperCase());
        m.setText(hasText ? text.trim() : "");
        m.setAttachmentId(attachmentId);
        m.setAttachmentName(attachmentName);
        m.setAttachmentContentType(attachmentContentType);
        m.setDurationSeconds(durationSeconds);
        m.setAttachmentSizeBytes(attachmentSizeBytes);
        m.setCreatedAt(Instant.now());
        m.setReadBy(new ArrayList<>(List.of(current)));

        CommunicationMessage saved = messageRepository.save(m);
        invalidateParticipantCache();
        return saved;
    }

    public CommunicationMessage announcement(String text) {
        if (!isAdmin()) throw new AccessDeniedException("Only Super Admin/Admin can send announcements");
        return send("GROUP", null, text, "ANNOUNCEMENT", null, null, null, null, null);
    }

    public CommunicationMessage markRead(String messageId) {
        String current = currentUsername();
        CommunicationMessage m = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        assertMessageAccess(m);
        List<String> readBy = m.getReadBy();
        if (readBy == null) { readBy = new ArrayList<>(); m.setReadBy(readBy); }
        if (!readBy.contains(current)) {
            readBy.add(current);
            CommunicationMessage saved = messageRepository.save(m);
            invalidateParticipantCache();
            return saved;
        }
        return m;
    }

    // ═══════════════════════════════════════════════════════════
    // File upload / download (unchanged)
    // ═══════════════════════════════════════════════════════════

    public Map<String, Object> upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required");
        if (file.getSize() > 50L * 1024L * 1024L) {
            throw new IllegalArgumentException("Maximum file size is 50 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null) contentType = "application/octet-stream";
        if (!isAllowedFileType(contentType, file.getOriginalFilename())) {
            throw new IllegalArgumentException("Unsupported file type");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) filename = "attachment";

        Document metadata = new Document();
        metadata.put("uploader", currentUsername());
        metadata.put("contentType", contentType);

        ObjectId id = gridFsTemplate.store(file.getInputStream(), filename, contentType, metadata);

        GridFSFile storedFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(id)));
        if (storedFile == null || storedFile.getLength() != file.getSize()) {
            try { gridFsTemplate.delete(Query.query(Criteria.where("_id").is(id))); } catch (Exception ignored) {}
            throw new IllegalStateException("Attachment upload incomplete");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("attachmentId", id.toHexString());
        result.put("fileName", filename);
        result.put("contentType", contentType);
        result.put("sizeBytes", file.getSize());
        return result;
    }

    public AttachmentData getAttachment(String attachmentId) {
        if (attachmentId == null || attachmentId.isBlank()) {
            throw new IllegalArgumentException("Attachment ID is required");
        }
        CommunicationMessage message = messageRepository.findByAttachmentId(attachmentId)
                .orElseThrow(() -> new AccessDeniedException("Attachment not found"));
        assertMessageAccess(message);

        ObjectId objectId;
        try { objectId = new ObjectId(attachmentId); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid attachment ID"); }

        GridFSFile file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(objectId)));
        if (file == null) throw new IllegalArgumentException("Attachment file not found");

        Long expectedSize = message.getAttachmentSizeBytes();
        if (expectedSize != null && expectedSize > 0 && file.getLength() != expectedSize) {
            throw new IllegalStateException("Stored attachment is incomplete");
        }

        GridFsResource resource = gridFsTemplate.getResource(file);
        String contentType = "application/octet-stream";
        if (file.getMetadata() != null) {
            String m = file.getMetadata().getString("contentType");
            if (m != null && !m.isBlank()) contentType = m;
        }
        return new AttachmentData(resource, message.getAttachmentName(), contentType, file.getLength());
    }

    // ═══════════════════════════════════════════════════════════
    // Unread counters — single-query, index-backed
    // ═══════════════════════════════════════════════════════════

    private long unreadGroupCount(String username) {
        try {
            return messageRepository.countUnreadGroup(username);
        } catch (Exception e) {
            return 0L;
        }
    }

    private long unreadDirectCount(String sender, String reader) {
        try {
            return messageRepository.countUnreadDirect(sender, reader);
        } catch (Exception e) {
            return 0L;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Super Admin monitoring
    // ═══════════════════════════════════════════════════════════

    public List<Map<String, Object>> getMonitorConversations() {
        if (!isAdmin()) throw new AccessDeniedException("Admin only");
        Map<String, Map<String, Object>> grouped = new java.util.LinkedHashMap<>();
        List<CommunicationMessage> directMessages =
                messageRepository.findByRecipientTypeOrderByCreatedAtAsc("DIRECT");
        for (CommunicationMessage m : directMessages) {
            String first = m.getSenderUsername();
            String second = m.getRecipientUsername();
            if (first == null || second == null) continue;
            if (telecallerRepository.findByUsername(first).isEmpty()) continue;
            if (telecallerRepository.findByUsername(second).isEmpty()) continue;

            String a = first.compareToIgnoreCase(second) <= 0 ? first : second;
            String b = first.compareToIgnoreCase(second) <= 0 ? second : first;
            String key = a + "::" + b;

            Map<String, Object> item = grouped.computeIfAbsent(key, k -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", key);
                map.put("firstUsername", a);
                map.put("secondUsername", b);
                map.put("firstName", getDisplayName(a));
                map.put("secondName", getDisplayName(b));
                return map;
            });
            item.put("lastMessageId", m.getId());
            item.put("lastMessageType", m.getType());
            item.put("lastMessageText", m.getText());
            item.put("lastMessageAt", m.getCreatedAt());
            item.put("lastSender", m.getSenderUsername());
        }
        return new ArrayList<>(grouped.values());
    }

    public List<CommunicationMessage> getMonitorMessages(String a, String b) {
        if (!isAdmin()) throw new AccessDeniedException("Admin only");
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            throw new IllegalArgumentException("Both usernames are required");
        }
        return messageRepository.findDirectConversation(a, b);
    }

    // ═══════════════════════════════════════════════════════════
    // Access / role helpers (unchanged)
    // ═══════════════════════════════════════════════════════════

    private void validateDirectChatAccess(String otherUsername) {
        if (otherUsername == null || otherUsername.isBlank()) {
            throw new IllegalArgumentException("Recipient is required");
        }
        String current = currentUsername();
        if (current.equals(otherUsername)) {
            throw new IllegalArgumentException("Cannot send a message to yourself");
        }
        boolean known =
                telecallerRepository.findByUsername(otherUsername).isPresent()
                        || userRepository.findByUsername(otherUsername).isPresent()
                        || employeeRepository.findByUsername(otherUsername).isPresent();
        if (!known) throw new IllegalArgumentException("Recipient not found");
    }

    private void assertMessageAccess(CommunicationMessage m) {
        String current = currentUsername();
        if (isAdmin()) return;
        if ("GROUP".equalsIgnoreCase(m.getRecipientType())) return;
        if (current.equals(m.getSenderUsername())) return;
        if (current.equals(m.getRecipientUsername())) return;
        throw new AccessDeniedException("You do not have access to this message");
    }

    private String getDisplayName(String username) {
        return userRepository.findByUsername(username)
                .map(User::getFullName)
                .filter(n -> n != null && !n.isBlank())
                .orElseGet(() -> telecallerRepository.findByUsername(username)
                        .map(Telecaller::getFullName)
                        .filter(n -> n != null && !n.isBlank())
                        .orElse(username));
    }

    private String getCurrentRole() {
        String role = authentication().getAuthorities().stream()
                .findFirst().map(a -> a.getAuthority()).orElse("ROLE_USER");
        if (role.startsWith("ROLE_")) role = role.substring(5);
        return role;
    }

    private boolean isAdminRole(String role) {
        if (role == null) return false;
        String n = normalizeRole(role);
        return "ADMIN".equals(n) || "SUPER_ADMIN".equals(n);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) return "USER";
        String n = role.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        if (n.startsWith("ROLE_")) n = n.substring(5);
        return n;
    }

    private boolean isAllowedFileType(String contentType, String filename) {
        return contentType != null && !contentType.isBlank();
    }

    private void invalidateParticipantCache() {
        synchronized (participantCache) {
            participantCache.clear();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Attachment DTO
    // ═══════════════════════════════════════════════════════════

    public static class AttachmentData {
        private final Resource resource;
        private final String fileName;
        private final String contentType;
        private final long sizeBytes;

        public AttachmentData(Resource resource, String fileName, String contentType, long sizeBytes) {
            this.resource = resource;
            this.fileName = fileName;
            this.contentType = contentType;
            this.sizeBytes = sizeBytes;
        }

        public Resource getResource() { return resource; }
        public String getFileName() { return fileName; }
        public String getContentType() { return contentType; }
        public long getSizeBytes() { return sizeBytes; }
    }
}