package com.indocrm.indocrm.controller;

import java.io.File;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.indocrm.indocrm.dto.PaymentRequest;
import com.indocrm.indocrm.model.Payment;
import com.indocrm.indocrm.service.PaymentService;

@RestController
@RequestMapping("/api/telecaller/payments")
@PreAuthorize("hasRole('TELECALLER')")
public class TelecallerPaymentController {

    private final PaymentService paymentService;

    public TelecallerPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Get all payment records created by the currently logged-in telecaller.
     */
    @GetMapping
    public List<Payment> all() {
        return paymentService.listForCurrentTelecaller();
    }

    /**
     * Create a new payment record.
     */
    @PostMapping
    public Payment create(@RequestBody PaymentRequest request) {
        return paymentService.create(request);
    }

    /**
     * Update a payment record.
     */
    @PutMapping("/{id}")
    public Payment update(
            @PathVariable String id,
            @RequestBody PaymentRequest request) {

        return paymentService.update(id, request);
    }

    /**
     * Get one payment record.
     */
    @GetMapping("/{id}")
    public Payment get(@PathVariable String id) {
        return paymentService.get(id);
    }

    /**
     * Download/view the generated invoice PDF.
     */
    @GetMapping("/{id}/invoice/pdf")
    public ResponseEntity<Resource> invoicePdf(@PathVariable String id) {

        File file = paymentService.invoicePdfFileForTelecaller(id);
        Resource resource = new FileSystemResource(file);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getName() + "\""
                )
                .body(resource);
    }

    /**
     * Send payment details to the client.
     */
    @PostMapping("/{id}/send-payment-details")
    public Payment sendPaymentDetails(
            @PathVariable String id,
            @RequestParam String channel) {

        return paymentService.recordPaymentDetailsSent(id, channel);
    }

    /**
     * Send the Super Admin generated invoice to the client.
     */
    @PostMapping("/{id}/send-invoice")
    public Payment sendInvoice(
            @PathVariable String id,
            @RequestParam String channel) {

        return paymentService.sendInvoiceToClient(id, channel);
    }
}

