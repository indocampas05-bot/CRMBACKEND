package com.indocrm.indocrm.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.indocrm.indocrm.dto.DemoRequest;
import com.indocrm.indocrm.model.Demo;
import com.indocrm.indocrm.service.DemoService;

@RestController
@RequestMapping("/api/demos")
public class DemoController {
    private final DemoService service;
    public DemoController(DemoService service){this.service=service;}
    @GetMapping public List<Demo> list(){return service.list();}
    @GetMapping("/{id}") public Demo get(@PathVariable String id){return service.get(id);}
    @PostMapping public ResponseEntity<?> create(@Valid @RequestBody DemoRequest request){try{return ResponseEntity.ok(service.create(request));}catch(Exception e){return ResponseEntity.badRequest().body(java.util.Map.of("message",e.getMessage()==null?"Unable to create demo":e.getMessage()));}}
    @PutMapping("/{id}") public ResponseEntity<?> update(@PathVariable String id,@Valid @RequestBody DemoRequest request){try{return ResponseEntity.ok(service.update(id,request));}catch(Exception e){return ResponseEntity.badRequest().body(java.util.Map.of("message",e.getMessage()==null?"Unable to update demo":e.getMessage()));}}
    @PatchMapping("/{id}/cancel") public Demo cancel(@PathVariable String id){return service.cancel(id);}
    @DeleteMapping("/{id}") public ResponseEntity<?> delete(@PathVariable String id){service.delete(id);return ResponseEntity.noContent().build();}
}

