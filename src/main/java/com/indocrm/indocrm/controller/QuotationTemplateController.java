package com.indocrm.indocrm.controller;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.indocrm.indocrm.dto.QuotationTemplateDesignRequest;
import com.indocrm.indocrm.dto.QuotationTemplateFieldsRequest;
import com.indocrm.indocrm.model.QuotationTemplate;
import com.indocrm.indocrm.service.QuotationTemplateService;
import com.indocrm.indocrm.service.TelecallerAccessService;

@RestController
@RequestMapping("/api/quotation-templates")
public class QuotationTemplateController {

    private final QuotationTemplateService service;
    private final TelecallerAccessService access;

    public QuotationTemplateController(QuotationTemplateService service, TelecallerAccessService access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public List<QuotationTemplate> list() { return service.list(access.currentUsername()); }

    @GetMapping("/{id}")
    public QuotationTemplate get(@PathVariable String id) { return service.get(id); }

    @PostMapping("/blank")
    public ResponseEntity<?> createBlank(@RequestParam(value = "name", required = false) String name) {
        try {
            return ResponseEntity.ok(service.createBlank(name, access.currentUsername()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "name", required = false) String name) {
        try {
            return ResponseEntity.ok(service.upload(file, name, access.currentUsername()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/fields")
    public ResponseEntity<?> saveFields(@PathVariable String id, @RequestBody QuotationTemplateFieldsRequest req) {
        try { return ResponseEntity.ok(service.saveFields(id, req.getFields())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PutMapping("/{id}/design")
    public ResponseEntity<?> saveDesign(@PathVariable String id, @RequestBody QuotationTemplateDesignRequest req) {
        try {
            return ResponseEntity.ok(service.saveDesign(id, req.getName(), req.getDesign(), req.getFields()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<FileSystemResource> preview(@PathVariable String id) throws Exception {
        File file = service.previewFile(id);
        if (!file.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        service.delete(id, access.currentUsername());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
