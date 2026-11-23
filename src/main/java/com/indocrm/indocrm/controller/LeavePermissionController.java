package com.indocrm.indocrm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.indocrm.indocrm.model.LeavePermissionRequest;
import com.indocrm.indocrm.service.LeavePermissionService;

@RestController
@RequestMapping("/api/leave-permissions")
public class LeavePermissionController {

    private final LeavePermissionService service;

    public LeavePermissionController(
            LeavePermissionService service) {

        this.service = service;
    }

    /*
     * ============================================================
     * TELECALLER
     * ============================================================
     */

    @GetMapping("/mine")
    @PreAuthorize("hasRole('TELECALLER')")
    public List<LeavePermissionRequest> mine() {

        return service.mine();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('TELECALLER')")
    public Map<String, Object> mySummary() {

        return service.mySummary();
    }

    @PostMapping
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<LeavePermissionRequest> create(
            @RequestBody LeavePermissionRequest request) {

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                service.create(request)
            );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TELECALLER')")
    public LeavePermissionRequest update(
            @PathVariable String id,
            @RequestBody LeavePermissionRequest request) {

        return service.updateMine(
            id,
            request
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<Void> delete(
            @PathVariable String id) {

        service.deleteMine(id);

        return ResponseEntity.noContent().build();
    }

    /*
     * ============================================================
     * SUPER ADMIN
     * ============================================================
     */

    @PostMapping("/admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<LeavePermissionRequest> adminCreate(@RequestBody LeavePermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adminCreate(request));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public LeavePermissionRequest adminUpdate(@PathVariable String id, @RequestBody LeavePermissionRequest request) {
        return service.adminUpdate(id, request);
    }

    @GetMapping("/admin")
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN')"
    )
    public List<LeavePermissionRequest> adminList(
            @RequestParam(
                required = false
            ) String status) {

        return service.adminList(status);
    }

    @GetMapping("/admin/summary")
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN')"
    )
    public Map<String, Object> adminSummary() {

        return service.adminSummary();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN')"
    )
    public LeavePermissionRequest updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) {

        return service.updateStatus(
            id,
            payload.get("status"),
            payload.get("rejectionReason")
        );
    }
}
