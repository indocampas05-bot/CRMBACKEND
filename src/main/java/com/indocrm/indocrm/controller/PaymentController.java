package com.indocrm.indocrm.controller;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.indocrm.indocrm.dto.InvoiceUpdateRequest;
import com.indocrm.indocrm.dto.PaymentRequest;
import com.indocrm.indocrm.dto.PaymentRecordRequest;
import com.indocrm.indocrm.model.PaymentQrCode;
import org.springframework.web.multipart.MultipartFile;
import com.indocrm.indocrm.model.Payment;
import com.indocrm.indocrm.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    /*
     * ============================================================
     * TELECALLER — own payment records
     * ============================================================
     */

    @GetMapping
    @PreAuthorize("hasRole('TELECALLER')")
    public List<Payment> list() { return service.listForCurrentTelecaller(); }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TELECALLER')")
    public Payment get(@PathVariable String id) { return service.get(id); }

    /** Record a Full/Partial/Advance payment against a lead. */
    @PostMapping
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<?> create(@RequestBody PaymentRequest r) {
        try { return ResponseEntity.ok(service.create(r)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody PaymentRequest r) {
        try { return ResponseEntity.ok(service.update(id, r)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    /** Telecaller shares the recorded payment/receipt with the client. Body: { "channel": "WhatsApp" | "Email" } */
    @PostMapping("/{id}/send")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<?> sendPaymentDetails(@PathVariable String id, @RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(service.recordPaymentDetailsSent(id, body.getOrDefault("channel", "WhatsApp"))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    /** Telecaller forwards the Super-Admin-generated invoice on to the client. */
    @PostMapping("/{id}/send-invoice")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<?> sendInvoiceToClient(@PathVariable String id, @RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(service.sendInvoiceToClient(id, body.getOrDefault("channel", "WhatsApp"))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    /** Telecaller: the exact invoice data (number, edited fields, items) the Super Admin saved. */
    @GetMapping("/{id}/invoice")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<?> invoiceData(@PathVariable String id) {
        try { return ResponseEntity.ok(service.invoiceForTelecaller(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage()))); }
    }

    @GetMapping("/{id}/invoice/pdf")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<FileSystemResource> invoicePdf(@PathVariable String id) {
        Payment p = service.get(id);
        File file = service.invoicePdfFileForTelecaller(id);
        if (!file.exists()) return ResponseEntity.notFound().build();
        return pdfResponse(file, p);
    }

    @GetMapping("/{id}/invoice/excel")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<FileSystemResource> invoiceExcel(@PathVariable String id) {
        Payment p = service.get(id);
        File file = service.invoiceExcelFileForTelecaller(id);
        if (!file.exists()) return ResponseEntity.notFound().build();
        return excelResponse(file, p);
    }


    /** Super Admin uploads one shared payment QR code. The latest upload is visible to every Telecaller. */
    @PostMapping(value = "/admin/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> uploadQr(@RequestParam("file") MultipartFile file) {
        try { PaymentQrCode qr = service.uploadQr(file); return ResponseEntity.ok(Map.of("id", qr.getId(), "fileName", qr.getFileName(), "contentType", qr.getContentType())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    /** Shared QR image for both Super Admin and Telecaller payment pages. */
    @GetMapping("/qr")
    @PreAuthorize("hasAnyRole('TELECALLER','SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> currentQr() {
        PaymentQrCode qr = service.currentQr();
        if (qr == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(qr.getContentType())).body(qr.getData());
    }

    @GetMapping("/qr/data")
    @PreAuthorize("hasAnyRole('TELECALLER','SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> currentQrData() {
        PaymentQrCode qr = service.currentQr();
        if (qr == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("dataUrl", "data:" + qr.getContentType() + ";base64," + java.util.Base64.getEncoder().encodeToString(qr.getData()), "fileName", qr.getFileName()));
    }

    @PostMapping("/{id}/payment-records")
    @PreAuthorize("hasRole('TELECALLER')")
    public ResponseEntity<?> addPaymentRecord(@PathVariable String id, @RequestBody PaymentRecordRequest r) {
        try { return ResponseEntity.ok(service.addPaymentRecord(id, r)); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    /*
     * ============================================================
     * SUPER ADMIN — verify, generate invoice, forward to telecaller
     * ============================================================
     */

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<Payment> adminList() { return service.adminList(); }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Payment adminGet(@PathVariable String id) { return service.adminGet(id); }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> adminUpdate(@PathVariable String id, @RequestBody PaymentRequest r) {
        try { return ResponseEntity.ok(service.adminUpdate(id, r)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }


    @PostMapping("/admin/{id}/payment-records")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> adminAddPaymentRecord(@PathVariable String id, @RequestBody PaymentRecordRequest r) {
        try { return ResponseEntity.ok(service.adminAddPaymentRecord(id, r)); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PutMapping("/admin/{id}/payment-records/{recordId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> adminUpdatePaymentRecord(@PathVariable String id, @PathVariable String recordId, @RequestBody PaymentRecordRequest r) {
        try { return ResponseEntity.ok(service.adminUpdatePaymentRecord(id, recordId, r)); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PostMapping("/admin/{id}/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> verify(@PathVariable String id) {
        try { return ResponseEntity.ok(service.verify(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PostMapping("/admin/{id}/generate-invoice")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> generateInvoice(@PathVariable String id) {
        try { return ResponseEntity.ok(service.generateInvoice(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PostMapping("/admin/{id}/send-to-telecaller")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> sendToTelecaller(@PathVariable String id) {
        try { return ResponseEntity.ok(service.sendInvoiceToTelecaller(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @GetMapping("/admin/{id}/invoice/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<FileSystemResource> adminInvoicePdf(@PathVariable String id) {
        Payment p = service.adminGet(id);
        File file = service.invoicePdfFileForAdmin(id);
        if (!file.exists()) return ResponseEntity.notFound().build();
        return pdfResponse(file, p);
    }

    @GetMapping("/admin/{id}/invoice/excel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<FileSystemResource> adminInvoiceExcel(@PathVariable String id) {
        Payment p = service.adminGet(id);
        File file = service.invoiceExcelFileForAdmin(id);
        if (!file.exists()) return ResponseEntity.notFound().build();
        return excelResponse(file, p);
    }

    /** Super Admin: load the generated invoice (number, date, client details, discount, tax...). */
    @GetMapping("/admin/{id}/invoice")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> adminInvoice(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getInvoice(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage()))); }
    }

    /** Super Admin: edit the generated invoice. PDF + Excel are regenerated from the edited data. */
    @PutMapping("/admin/{id}/invoice")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> adminUpdateInvoice(@PathVariable String id, @RequestBody InvoiceUpdateRequest r) {
        try { return ResponseEntity.ok(service.updateInvoice(id, r)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage()))); }
    }

    private ResponseEntity<FileSystemResource> excelResponse(File file, Payment p) {
        String fileName = "Invoice_" + (p.getInvoiceId() != null ? p.getInvoiceId() : p.getId()) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(new FileSystemResource(file));
    }

    private ResponseEntity<FileSystemResource> pdfResponse(File file, Payment p) {
        String fileName = "Invoice_" + (p.getInvoiceId() != null ? p.getInvoiceId() : p.getId()) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(new FileSystemResource(file));
    }
}