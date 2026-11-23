package com.indocrm.indocrm.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.indocrm.indocrm.model.InvoiceAsset;
import com.indocrm.indocrm.service.InvoiceAssetService;

/**
 * Fixes the 404 on:
 *   POST /api/payments/admin/invoice-assets/logo | signature | thank-you
 *   GET  /api/payments/invoice-assets/data
 */
@RestController
@RequestMapping("/api/payments")
public class InvoiceAssetController {

    private final InvoiceAssetService service;

    public InvoiceAssetController(InvoiceAssetService service) {
        this.service = service;
    }

    @PostMapping(value = "/admin/invoice-assets/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> upload(@PathVariable String type, @RequestParam("file") MultipartFile file) {
        try {
            InvoiceAsset a = service.save(type, file);
            return ResponseEntity.ok(Map.of("id", a.getId(), "type", a.getType(),
                    "fileName", String.valueOf(a.getFileName())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/invoice-assets/data")
    @PreAuthorize("hasAnyRole('TELECALLER','SUPER_ADMIN','ADMIN')")
    public Map<String, String> data() {
        return service.dataUrls();
    }
}