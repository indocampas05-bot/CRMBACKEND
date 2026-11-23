// FILE: src/main/java/com/indocrm/indocrm/controller/AgreementPlanController.java
// NEW FILE
package com.indocrm.indocrm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.indocrm.indocrm.dto.AgreementPlanRequest;
import com.indocrm.indocrm.model.AgreementPlan;
import com.indocrm.indocrm.service.AgreementPlanService;

@RestController
@RequestMapping("/api/agreement-plans")
public class AgreementPlanController {

    private final AgreementPlanService s;

    public AgreementPlanController(AgreementPlanService s) {
        this.s = s;
    }

    /** GET /api/agreement-plans?type=ERP  or  ?type=DIGITAL_MARKETING */
    @GetMapping
    public List<AgreementPlan> list(@RequestParam(required = false) String type) {
        return s.listByType(type);
    }

    @GetMapping("/{id}")
    public AgreementPlan get(@PathVariable String id) {
        return s.get(id);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AgreementPlanRequest r) {
        return call(() -> s.create(r));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody AgreementPlanRequest r) {
        return call(() -> s.update(id, r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> archive(@PathVariable String id) {
        return call(() -> { s.archive(id); return Map.of("success", true); });
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
