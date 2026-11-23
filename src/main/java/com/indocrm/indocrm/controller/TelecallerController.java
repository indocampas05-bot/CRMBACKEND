// FILE: src/main/java/com/indocrm/indocrm/controller/TelecallerController.java

package com.indocrm.indocrm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.service.TelecallerService;

@RestController
@RequestMapping("/api/telecallers")
public class TelecallerController {

    @Autowired
    private TelecallerService telecallerService;

    // NEW: self-service profile lookup for the currently logged-in telecaller.
    // Unlike the other endpoints here (which are for admins managing telecallers),
    // this one is meant to be called BY the telecaller's own app so it can refresh
    // its assigned districts (e.g. after a Super Admin reassigns a district)
    // without forcing the telecaller to log out and back in.
    @GetMapping("/me")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<Telecaller> getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return telecallerService.getTelecallerByUsername(auth.getName())
                .map(tc -> {
                    // never leak the password hash to the client
                    tc.setPassword(null);
                    return ResponseEntity.ok(tc);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Telecaller>> getAllTelecallers() {
        return ResponseEntity.ok(telecallerService.getAllTelecallers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Telecaller> getTelecaller(@PathVariable String id) {
        return telecallerService.getTelecallerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Telecaller> getTelecallerByUsername(@PathVariable String username) {
        return telecallerService.getTelecallerByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/phone/{phoneNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Telecaller> getByPhoneNumber(@PathVariable String phoneNumber) {
        return telecallerService.findByAssignedPhoneNumber(phoneNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Telecaller> getTelecallerByEmployeeId(@PathVariable String employeeId) {
        return telecallerService.getTelecallerByEmployeeId(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Telecaller> createTelecaller(@RequestBody Telecaller telecaller) {
        Telecaller created = telecallerService.createTelecaller(telecaller);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Telecaller> updateTelecaller(@PathVariable String id, @RequestBody Telecaller telecaller) {
        Telecaller updated = telecallerService.updateTelecaller(id, telecaller);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteTelecaller(@PathVariable String id) {
        telecallerService.deleteTelecaller(id);
        return ResponseEntity.ok(Map.of("message", "Telecaller deleted successfully"));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable String id, @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("newPassword");
        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password is required"));
        }
        telecallerService.resetPassword(id, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Telecaller> toggleStatus(@PathVariable String id) {
        Telecaller updated = telecallerService.toggleStatus(id);
        return ResponseEntity.ok(updated);
    }

    // NEW: Assign phone number to telecaller
    @PostMapping("/{id}/assign-phone")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Telecaller> assignPhoneNumber(
            @PathVariable String id, 
            @RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Telecaller updated = telecallerService.assignPhoneNumber(id, phoneNumber);
        return ResponseEntity.ok(updated);
    }

    // NEW: Unassign phone number
    @PostMapping("/{id}/unassign-phone")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Telecaller> unassignPhoneNumber(@PathVariable String id) {
        Telecaller updated = telecallerService.unassignPhoneNumber(id);
        return ResponseEntity.ok(updated);
    }
}

