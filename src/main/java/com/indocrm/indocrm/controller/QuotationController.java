package com.indocrm.indocrm.controller;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.indocrm.indocrm.dto.QuotationRequest;
import com.indocrm.indocrm.model.Quotation;
import com.indocrm.indocrm.service.QuotationService;

@RestController
@RequestMapping("/api/quotations")
public class QuotationController {

    private final QuotationService service;

    public QuotationController(QuotationService service) {
        this.service = service;
    }

    /*
     * ============================================================
     * TELECALLER — own quotations
     * ============================================================
     */

    @GetMapping
    @PreAuthorize("hasRole('TELECALLER')")
    public List<Quotation> list() { return service.list(); }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TELECALLER')")
    public Quotation get(@PathVariable String id) { return service.get(id); }

    @PostMapping
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<?> create(@RequestBody QuotationRequest r) {
        try { return ResponseEntity.ok(service.create(r)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody QuotationRequest r) {
        try { return ResponseEntity.ok(service.update(id, r)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(service.updateStatus(id, body.get("status"))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    /** Telecaller: send the Draft quotation to the Super Admin for approval. */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<?> submit(@PathVariable String id) {
        try { return ResponseEntity.ok(service.submitForApproval(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    /** Telecaller: send an Approved quotation on to the lead (WhatsApp/email/etc). */
    @PostMapping("/{id}/mark-sent")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<?> markSent(@PathVariable String id, @RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(service.markSent(id, body.getOrDefault("channel", "message"))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<?> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<FileSystemResource> pdf(@PathVariable String id) {
        Quotation q = service.get(id);
        File file = service.pdfFile(id);
        if (!file.exists()) return ResponseEntity.notFound().build();
        String fileName = q.getGeneratedPdfFileName() != null ? q.getGeneratedPdfFileName() : "Quotation.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(new FileSystemResource(file));
    }

    /*
     * ============================================================
     * SUPER ADMIN — cross-telecaller visibility + approve/reject
     * ============================================================
     */

    /** GET /api/quotations/admin?status=Pending%20Approval  (status optional, "All" or omitted = everything) */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<Quotation> adminList(@RequestParam(required = false) String status) {
        return service.adminList(status);
    }

    @GetMapping("/admin/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Object> adminSummary() {
        return service.adminSummary();
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Quotation adminGet(@PathVariable String id) {
        return service.getForAdmin(id);
    }

    @GetMapping("/admin/{id}/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<FileSystemResource> adminPdf(@PathVariable String id) {
        Quotation q = service.getForAdmin(id);
        File file = service.pdfFileForAdmin(id);
        if (!file.exists()) return ResponseEntity.notFound().build();
        String fileName = q.getGeneratedPdfFileName() != null ? q.getGeneratedPdfFileName() : "Quotation.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(new FileSystemResource(file));
    }

    /** Body: { "status": "Approved" | "Rejected", "rejectionReason": "..." (required when Rejected) } */
    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> review(@PathVariable String id, @RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(service.review(id, body.get("status"), body.get("rejectionReason"))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }
}