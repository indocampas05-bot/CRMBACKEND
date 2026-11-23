package com.indocrm.indocrm.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.indocrm.indocrm.service.GoogleMeetService;
import com.indocrm.indocrm.service.TelecallerAccessService;

@RestController
@RequestMapping("/api/google-meet")
public class GoogleMeetController {
    private final GoogleMeetService service; private final TelecallerAccessService access;
    public GoogleMeetController(GoogleMeetService service, TelecallerAccessService access){this.service=service;this.access=access;}
    @GetMapping("/connect-url") public Map<String,Object> connectUrl(){return Map.of("authorizationUrl",service.authorizationUrl(access.currentUsername()));}
    @GetMapping("/status") public Map<String,Object> status(){return Map.of("connected",service.connected(access.currentUsername()));}
    @PostMapping("/create") public ResponseEntity<?> create(){try{return ResponseEntity.ok(Map.of("meetingUri",service.createMeeting(access.currentUsername()),"success",true));}catch(GoogleMeetService.GoogleNotConnectedException e){return ResponseEntity.status(409).body(Map.of("success",false,"connected",false,"authorizationUrl",e.getAuthorizationUrl(),"message","Connect Google Meet first"));}catch(Exception e){return ResponseEntity.internalServerError().body(Map.of("success",false,"message",e.getMessage()));}}
    @GetMapping("/oauth/callback") public ResponseEntity<?> callback(@RequestParam String code,@RequestParam String state){try{service.handleCallback(code,state); return ResponseEntity.ok().header("Content-Type","text/html;charset=UTF-8").body("<html><body><script>window.close();</script><h3>Google Meet connected. You can close this window.</h3></body></html>");}catch(Exception e){return ResponseEntity.badRequest().body("Google OAuth failed: "+e.getMessage());}}
}

