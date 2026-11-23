// FILE: src/main/java/com/indocrm/indocrm/controller/AgreementController.java
// REPLACES existing file — adds POST /preview and GET /{id}/pdf.
package com.indocrm.indocrm.controller;
import java.io.File;
import java.util.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.indocrm.indocrm.dto.AgreementRequest;
import com.indocrm.indocrm.model.Agreement;
import com.indocrm.indocrm.service.AgreementService;

@RestController
@RequestMapping("/api/agreements")
public class AgreementController {

    private final AgreementService s;

    public AgreementController(AgreementService x) { s = x; }

    @GetMapping
    public List<Agreement> list(@RequestParam(required = false) String status,
                                 @RequestParam(required = false) String telecaller,
                                 @RequestParam(required = false) String leadId) {
        return s.list(status, telecaller, leadId);
    }

    @GetMapping("/{id}")
    public Agreement get(@PathVariable String id) {
        return s.get(id);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AgreementRequest r) {
        return call(() -> s.create(r));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody AgreementRequest r) {
        return call(() -> s.update(id, r));
    }

    /**
     * NEW: renders the merged agreement (content + pricing) without saving
     * anything — backs the "Preview Agreement" step on the Add Agreement form.
     */
    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestBody AgreementRequest r) {
        return call(() -> s.preview(r));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable String id) {
        return call(() -> s.submit(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable String id, @RequestBody(required = false) Map<String, String> b) {
        return call(() -> s.approve(id, b == null ? null : b.get("remark")));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable String id, @RequestBody Map<String, String> b) {
        return call(() -> s.reject(id, b.get("reason")));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<?> send(@PathVariable String id, @RequestBody(required = false) Map<String, String> b) {
        return call(() -> s.send(id, b == null ? "WhatsApp" : b.getOrDefault("channel", "WhatsApp")));
    }

    /** NEW: download / print the generated PDF (generated on demand if missing). */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> pdf(@PathVariable String id) {
        try {
            Agreement a = s.get(id);
            File file = s.pdfFile(id);
            String fileName = a.getGeneratedPdfFileName() != null ? a.getGeneratedPdfFileName() : "Agreement.pdf";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline().filename(fileName).build().toString())
                    .body(new FileSystemResource(file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() == null ? "PDF not available" : e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> remove(@PathVariable String id) {
        return call(() -> { s.delete(id); return Map.of("success", true); });
    }

    private ResponseEntity<?> call(Supplier x) {
        try {
            return ResponseEntity.ok(x.get());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() == null ? "Request failed" : e.getMessage()));
        }
    }

    @FunctionalInterface
    interface Supplier { Object get() throws Exception; }
}
