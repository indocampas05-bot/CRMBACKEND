// FILE: src/main/java/com/indocrm/indocrm/controller/AgreementCategoryController.java
// NEW FILE
//   GET  /api/agreement-categories            -> the 5 categories (no HTML)
//   GET  /api/agreement-categories/{key}      -> one category incl. editable HTML
//   PUT  /api/agreement-categories/{key}      -> save edited HTML   (SUPER_ADMIN / ADMIN)
//   POST /api/agreement-categories/{key}/reset-> restore default    (SUPER_ADMIN / ADMIN)
package com.indocrm.indocrm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.indocrm.indocrm.service.AgreementCategoryService;

@RestController
@RequestMapping("/api/agreement-categories")
public class AgreementCategoryController {

    private final AgreementCategoryService s;

    public AgreementCategoryController(AgreementCategoryService s) {
        this.s = s;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return s.list();
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> get(@PathVariable String key) {
        return call(() -> s.get(key));
    }

    @PutMapping("/{key}")
    public ResponseEntity<?> save(@PathVariable String key, @RequestBody Map<String, String> body) {
        return call(() -> s.save(key, body.get("contentHtml")));
    }

    @PostMapping("/{key}/reset")
    public ResponseEntity<?> reset(@PathVariable String key) {
        return call(() -> s.reset(key));
    }

    private ResponseEntity<?> call(java.util.function.Supplier<Object> x) {
        try {
            return ResponseEntity.ok(x.get());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() == null ? "Request failed" : e.getMessage()));
        }
    }
}
