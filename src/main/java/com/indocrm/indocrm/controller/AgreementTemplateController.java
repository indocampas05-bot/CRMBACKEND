// FILE: src/main/java/com/indocrm/indocrm/controller/AgreementTemplateController.java
// REPLACES existing file — list() now accepts an optional ?type= filter
// ("ERP" or "DIGITAL_MARKETING") so the frontend can auto-select the one
// master template for the chosen agreement type. Everything else unchanged.
package com.indocrm.indocrm.controller;
import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile; import com.indocrm.indocrm.model.AgreementTemplate; import com.indocrm.indocrm.service.AgreementTemplateService;
@RestController @RequestMapping("/api/agreement-templates") public class AgreementTemplateController {
 private final AgreementTemplateService s; public AgreementTemplateController(AgreementTemplateService x){s=x;}
 @GetMapping public List<AgreementTemplate> list(@RequestParam(required=false) String type){return s.listByType(type);}
 @GetMapping("/{id}") public AgreementTemplate get(@PathVariable String id){return s.get(id);}
 @PostMapping public ResponseEntity<?> create(@RequestBody Map<String,String>b){return call(()->s.create(b.get("name"),b.get("contentHtml")));}
 @PostMapping(value="/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<?> upload(@RequestParam(required=false)String name,@RequestParam(required=false)String contentHtml,@RequestPart("file")MultipartFile file){return call(()->s.upload(name,contentHtml,file));}
 @PutMapping("/{id}/html") public ResponseEntity<?> html(@PathVariable String id,@RequestBody Map<String,String>b){return call(()->s.updateHtml(id,b.get("contentHtml")));}
 @PutMapping("/{id}/name") public ResponseEntity<?> name(@PathVariable String id,@RequestBody Map<String,String>b){return call(()->s.rename(id,b.get("name")));}
 @DeleteMapping("/{id}") public ResponseEntity<?> archive(@PathVariable String id){return call(()->{s.archive(id);return Map.of("message","Template archived");});}
 private ResponseEntity<?> call(Supplier x){try{return ResponseEntity.ok(x.get());}catch(Exception e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()==null?"Request failed":e.getMessage()));}} @FunctionalInterface interface Supplier{Object get()throws Exception;}
}
