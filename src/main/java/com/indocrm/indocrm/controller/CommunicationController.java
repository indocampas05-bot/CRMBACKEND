package com.indocrm.indocrm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.indocrm.indocrm.model.CommunicationMessage;
import com.indocrm.indocrm.service.CommunicationService;

@RestController
@RequestMapping("/api/communications")
public class CommunicationController {

    private final CommunicationService service;

    public CommunicationController(
            CommunicationService service) {

        this.service = service;
    }

    // ============================================================
    // PARTICIPANTS
    // ============================================================

    @GetMapping("/participants")
    public ResponseEntity<?> participants() {

        return ResponseEntity.ok(
                service.getParticipants()
        );
    }

    // ============================================================
    // MESSAGES
    // ============================================================

    @GetMapping("/messages")
    public ResponseEntity<List<CommunicationMessage>> messages(
            @RequestParam(
                    defaultValue = "GROUP-ALL"
            )
            String chatId) {

        return ResponseEntity.ok(
                service.getMessages(chatId)
        );
    }

    // ============================================================
    // SEND MESSAGE
    // ============================================================

    @PostMapping("/messages")
    public ResponseEntity<CommunicationMessage> send(
            @RequestBody Map<String, Object> body) {

        String recipientType =
                stringValue(
                        body.get("recipientType")
                );

        String recipientUsername =
                stringValue(
                        body.get("recipientUsername")
                );

        String text =
                stringValue(
                        body.get("text")
                );

        String type =
                stringValue(
                        body.get("type")
                );

        String attachmentId =
                stringValue(
                        body.get("attachmentId")
                );

        String attachmentName =
                stringValue(
                        body.get("attachmentName")
                );

        String attachmentContentType =
                stringValue(
                        body.get("attachmentContentType")
                );

        Integer durationSeconds =
                integerValue(
                        body.get("durationSeconds")
                );

        Long attachmentSizeBytes =
                longValue(
                        body.get("attachmentSizeBytes")
                );

        CommunicationMessage message =
                service.send(
                        recipientType,
                        recipientUsername,
                        text,
                        type,
                        attachmentId,
                        attachmentName,
                        attachmentContentType,
                        durationSeconds,
                        attachmentSizeBytes
                );

        return ResponseEntity.ok(
                message
        );
    }

    // ============================================================
    // ANNOUNCEMENT
    // ============================================================

    @PostMapping("/announcements")
    public ResponseEntity<CommunicationMessage> announcement(
            @RequestBody Map<String, String> body) {

        CommunicationMessage message =
                service.announcement(
                        body.get("text")
                );

        return ResponseEntity.ok(
                message
        );
    }

    // ============================================================
    // MONITOR TELECALLER DIRECT CHATS
    // ============================================================

    @GetMapping("/monitor/conversations")
    public ResponseEntity<?> monitorConversations() {

        return ResponseEntity.ok(
                service.getMonitorConversations()
        );
    }

    @GetMapping("/monitor/messages")
    public ResponseEntity<List<CommunicationMessage>> monitorMessages(
            @RequestParam String firstUsername,
            @RequestParam String secondUsername) {

        return ResponseEntity.ok(
                service.getMonitorMessages(
                        firstUsername,
                        secondUsername
                )
        );
    }

    // ============================================================
    // MARK READ
    // ============================================================

    @PatchMapping("/messages/{id}/read")
    public ResponseEntity<CommunicationMessage> markRead(
            @PathVariable String id) {

        return ResponseEntity.ok(
                service.markRead(id)
        );
    }

    // ============================================================
    // FILE / VOICE UPLOAD
    // ============================================================

    @PostMapping(
            value = "/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> upload(
            @RequestParam("file")
            MultipartFile file) {

        try {

            if (file == null || file.isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                Map.of(
                                        "message",
                                        "Uploaded file is empty"
                                )
                        );
            }

            return ResponseEntity.ok(
                    service.upload(file)
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    e.getMessage() != null
                                            ? e.getMessage()
                                            : "File upload failed"
                            )
                    );
        }
    }

    // ============================================================
    // FILE / VOICE DOWNLOAD
    // ============================================================

    @GetMapping("/attachments/{id}")
    public ResponseEntity<Resource> attachment(
            @PathVariable String id) {

        CommunicationService.AttachmentData data =
                service.getAttachment(id);

        MediaType mediaType;

        try {

            String contentType =
                    data.getContentType();

            if (contentType == null ||
                    contentType.isBlank()) {

                mediaType =
                        MediaType.APPLICATION_OCTET_STREAM;

            } else {

                mediaType =
                        MediaType.parseMediaType(
                                contentType
                        );
            }

        } catch (Exception e) {

            mediaType =
                    MediaType.APPLICATION_OCTET_STREAM;
        }

        String fileName =
                data.getFileName() != null &&
                !data.getFileName().isBlank()
                        ? data.getFileName()
                        : "attachment";

        ResponseEntity.BodyBuilder response =
                ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(
                                HttpHeaders.ACCEPT_RANGES,
                                "bytes"
                        )
                        .header(
                                HttpHeaders.CACHE_CONTROL,
                                "no-store, no-cache, must-revalidate, max-age=0"
                        )
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition
                                        .inline()
                                        .filename(fileName)
                                        .build()
                                        .toString()
                        );

        /*
         * IMPORTANT:
         *
         * getSizeBytes() does NOT throw IOException.
         * Therefore there must NOT be:
         *
         * catch (IOException ...)
         *
         * here.
         */

        long contentLength =
                data.getSizeBytes();

        if (contentLength >= 0) {

            response.contentLength(
                    contentLength
            );
        }

        return response.body(
                data.getResource()
        );
    }

    // ============================================================
    // LONG VALUE
    // ============================================================

    private Long longValue(
            Object value) {

        if (value == null) {
            return null;
        }

        try {

            return Long.valueOf(
                    String.valueOf(value)
            );

        } catch (NumberFormatException e) {

            return null;
        }
    }

    // ============================================================
    // INTEGER VALUE
    // ============================================================

    private Integer integerValue(
            Object value) {

        if (value == null) {
            return null;
        }

        try {

            return Integer.valueOf(
                    String.valueOf(value)
            );

        } catch (NumberFormatException e) {

            return null;
        }
    }

    // ============================================================
    // STRING VALUE
    // ============================================================

    private String stringValue(
            Object value) {

        return value == null
                ? null
                : String.valueOf(value);
    }
}