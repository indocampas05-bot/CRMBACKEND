package com.indocrm.indocrm.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.indocrm.indocrm.model.AttendanceAuditLog;
import com.indocrm.indocrm.model.AttendanceSession;
import com.indocrm.indocrm.service.AttendanceService;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService service;
    public AttendanceController(AttendanceService service){this.service=service;}

    @PostMapping("/login")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<Map<String,Object>> login(Authentication auth, HttpServletRequest request,
                                                     @RequestBody(required=false) Map<String,Object> body){
        return ResponseEntity.ok(service.login(auth.getName(), device(body), request.getRemoteAddr()));
    }

    @PostMapping("/logout")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<Map<String,Object>> logout(Authentication auth){ return ResponseEntity.ok(service.manualLogout(auth.getName())); }

    @PostMapping("/heartbeat")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<Map<String,Object>> heartbeat(Authentication auth){ return ResponseEntity.ok(service.heartbeat(auth.getName())); }

    @PostMapping("/reconnect")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<Map<String,Object>> reconnect(Authentication auth, HttpServletRequest request,
                                                         @RequestBody(required=false) Map<String,Object> body){
        return ResponseEntity.ok(service.reconnect(auth.getName(), device(body), request.getRemoteAddr(), false));
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<Map<String,Object>> today(Authentication auth){return ResponseEntity.ok(service.today(auth.getName()));}

    @GetMapping("/mine")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<List<Map<String,Object>>> mine(Authentication auth){return ResponseEntity.ok(service.mine(auth.getName()));}

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<Map<String,Object>>> admin(@RequestParam(required=false) String date){return ResponseEntity.ok(service.admin(date));}

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String,Object>> detail(@PathVariable String id){return ResponseEntity.ok(service.adminDetail(id));}

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String,Object>> edit(@PathVariable String id,@RequestBody Map<String,Object> body){return ResponseEntity.ok(service.adminEdit(id,body));}

    @GetMapping("/admin/{id}/sessions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<AttendanceSession>> sessions(@PathVariable String id){return ResponseEntity.ok(service.sessions(id));}

    @GetMapping("/admin/{id}/audits")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<AttendanceAuditLog>> audits(@PathVariable String id){return ResponseEntity.ok(service.audits(id));}

    private String device(Map<String,Object> body){return body==null?null:String.valueOf(body.getOrDefault("deviceInfo",""));}
}
