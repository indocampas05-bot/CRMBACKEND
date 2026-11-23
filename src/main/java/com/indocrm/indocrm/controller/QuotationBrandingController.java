package com.indocrm.indocrm.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.indocrm.indocrm.service.QuotationBrandingService;

/**
 * Quotation letterhead (logo / letterpad / footer) managed by the Super Admin from the Quotations page.
 *   GET    /api/quotation-branding
 *   POST   /api/quotation-branding/{logo|letterpad}   (multipart "file")
 *   DELETE /api/quotation-branding/{logo|letterpad}
 *   PUT    /api/quotation-branding/settings           {showBrandText, phone, website, address}
 */
@RestController
@RequestMapping("/api/quotation-branding")
public class QuotationBrandingController {

    private final QuotationBrandingService service;

    public QuotationBrandingController(QuotationBrandingService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('TELECALLER','SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> status() {
        try { return ResponseEntity.ok(service.status()); }
        catch (Exception e) { return bad(e); }
    }

    @PostMapping(value = "/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> upload(@PathVariable String type, @RequestParam("file") MultipartFile file) {
        try { return ResponseEntity.ok(service.save(type, file)); }
        catch (Exception e) { return bad(e); }
    }

    @DeleteMapping("/{type}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> remove(@PathVariable String type) {
        try { return ResponseEntity.ok(service.delete(type)); }
        catch (Exception e) { return bad(e); }
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> settings(@RequestBody Map<String, Object> body) {
        try { return ResponseEntity.ok(service.saveSettings(body)); }
        catch (Exception e) { return bad(e); }
    }

    private ResponseEntity<?> bad(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage())));
    }
}