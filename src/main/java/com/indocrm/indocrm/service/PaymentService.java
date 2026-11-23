package com.indocrm.indocrm.service;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.indocrm.indocrm.dto.InvoiceUpdateRequest;
import com.indocrm.indocrm.dto.PaymentRecordRequest;
import com.indocrm.indocrm.dto.PaymentRequest;
import com.indocrm.indocrm.model.Invoice;
import com.indocrm.indocrm.model.Lead;
import com.indocrm.indocrm.model.Payment;
import com.indocrm.indocrm.model.PaymentQrCode;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.repository.InvoiceRepository;
import com.indocrm.indocrm.repository.PaymentQrCodeRepository;
import com.indocrm.indocrm.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final InvoiceRepository invoiceRepo;
    private final TelecallerAccessService accessService;
    private final InvoicePdfService pdfService;
    private final InvoiceExcelService excelService;
    private final PaymentQrCodeRepository qrRepo;
    private final InvoiceNumberService invoiceNumberService;

    private static final String VERIFICATION_PENDING = "Pending";
    private static final String VERIFICATION_VERIFIED = "Verified";

    private static final String INVOICE_NOT_GENERATED = "Not Generated";
    private static final String INVOICE_GENERATED = "Generated";
    private static final String INVOICE_SENT_TO_TELECALLER =
            "Sent to Telecaller";
    private static final String INVOICE_SENT_TO_CLIENT =
            "Sent to Client";

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter
                    .ofPattern("dd MMM yyyy · hh:mm a")
                    .withZone(ZoneId.systemDefault());

    public PaymentService(
            PaymentRepository paymentRepo,
            InvoiceRepository invoiceRepo,
            TelecallerAccessService accessService,
            InvoicePdfService pdfService,
            InvoiceExcelService excelService,
            PaymentQrCodeRepository qrRepo,
            InvoiceNumberService invoiceNumberService
    ) {
        this.paymentRepo = paymentRepo;
        this.invoiceRepo = invoiceRepo;
        this.accessService = accessService;
        this.pdfService = pdfService;
        this.excelService = excelService;
        this.qrRepo = qrRepo;
        this.invoiceNumberService = invoiceNumberService;
    }

    /*
     * ============================================================
     * TELECALLER
     * ============================================================
     */

    public List<Payment> listForCurrentTelecaller() {
        return paymentRepo.findByCreatedBy(
                accessService.currentUsername()
        );
    }

    public Payment get(String id) {

        Payment payment = paymentRepo.findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Payment not found: " + id
                        )
                );

        if (accessService.isTelecaller()) {

            if (payment.getCreatedBy() == null
                    || !payment.getCreatedBy().equals(
                            accessService.currentUsername()
                    )) {

                throw new AccessDeniedException(
                        "You do not have access to this payment record"
                );
            }
        }

        return payment;
    }

    /*
     * ============================================================
     * CREATE PAYMENT
     * ============================================================
     */

    public Payment create(PaymentRequest request) {

        if (request.getLeadId() == null
                || request.getLeadId().isBlank()) {

            throw new IllegalArgumentException(
                    "leadId is required"
            );
        }

        Lead lead = accessService.requireLeadAccess(
                request.getLeadId()
        );

        Telecaller telecaller =
                accessService.currentTelecaller();

        Payment payment = new Payment();

        payment.setLeadId(request.getLeadId());

        payment.setOrganizationName(
                firstNonBlank(
                        request.getOrganizationName(),
                        lead.getSchool(),
                        lead.getName()
                )
        );

        payment.setAddress(
                firstNonBlank(
                        request.getAddress(),
                        lead.getSchoolAddress()
                )
        );

        payment.setPhone(
                firstNonBlank(
                        request.getPhone(),
                        lead.getPhoneNumber(),
                        lead.getMobile()
                )
        );

        payment.setEmail(
                firstNonBlank(
                        request.getEmail(),
                        lead.getEmail()
                )
        );

        payment.setPaymentType(
                request.getPaymentType() != null
                        ? request.getPaymentType()
                        : Payment.TYPE_FULL
        );

        payment.setTotalAmount(
                request.getTotalAmount() != null
                        ? request.getTotalAmount()
                        : 0.0
        );

        payment.setAmountPaid(0.0);

        payment.setBalanceAmount(
                n(payment.getTotalAmount())
        );

        payment.setPaymentDate(
                request.getPaymentDate()
        );

        payment.setNotes(
                request.getNotes()
        );

        payment.setStatus(
                Payment.STATUS_RECORDED
        );

        payment.setVerificationStatus(
                VERIFICATION_PENDING
        );

        payment.setInvoiceStatus(
                INVOICE_NOT_GENERATED
        );

        payment.setCreatedBy(
                accessService.currentUsername()
        );

        /*
         * Initial payment record.
         */
        if (request.getAmountPaid() != null
                && request.getAmountPaid() > 0) {

            double amount =
                    request.getAmountPaid();

            if (amount > n(payment.getTotalAmount())) {

                throw new IllegalArgumentException(
                        "Payment amount cannot exceed total amount"
                );
            }

            Payment.PaymentRecord record =
                    toRecord(
                            request,
                            accessService.currentUsername()
                    );

            payment.getPaymentHistory().add(
                    record
            );
        }

        recalculate(payment);

        payment.setTelecallerName(
                telecaller.getFullName() != null
                        ? telecaller.getFullName()
                        : telecaller.getUsername()
        );

        addTimeline(
                payment,
                payment.getPaymentType()
                        + " payment of "
                        + fmt(payment.getAmountPaid())
                        + " recorded",
                "check",
                "#dcfce7",
                "#15803d"
        );

        return paymentRepo.save(payment);
    }

    /*
     * ============================================================
     * UPDATE PAYMENT
     * ============================================================
     */

    public Payment update(
            String id,
            PaymentRequest request
    ) {

        Payment payment = get(id);

        if (request.getOrganizationName() != null) {
            payment.setOrganizationName(
                    request.getOrganizationName()
            );
        }

        if (request.getAddress() != null) {
            payment.setAddress(
                    request.getAddress()
            );
        }

        if (request.getPhone() != null) {
            payment.setPhone(
                    request.getPhone()
            );
        }

        if (request.getEmail() != null) {
            payment.setEmail(
                    request.getEmail()
            );
        }

        boolean locked =
                isLockedForTelecallerAmountEdits(
                        payment.getStatus()
                );

        boolean changingAmount =
                request.getTotalAmount() != null
                        && Double.compare(
                                n(request.getTotalAmount()),
                                n(payment.getTotalAmount())
                        ) != 0;

        boolean changingType =
                request.getPaymentType() != null
                        && !request.getPaymentType().equals(
                                payment.getPaymentType()
                        );

        if (locked
                && (changingAmount || changingType)
                && accessService.isTelecaller()) {

            throw new IllegalStateException(
                    "This payment has already been verified. "
                            + "Only the Super Admin can change "
                            + "the total amount or payment type."
            );
        }

        if (request.getPaymentType() != null) {
            payment.setPaymentType(
                    request.getPaymentType()
            );
        }

        if (request.getTotalAmount() != null) {

            if (request.getTotalAmount()
                    < n(payment.getAmountPaid())) {

                throw new IllegalArgumentException(
                        "Total amount cannot be less than "
                                + "the amount already paid"
                );
            }

            payment.setTotalAmount(
                    request.getTotalAmount()
            );
        }

        if (request.getPaymentDate() != null) {
            payment.setPaymentDate(
                    request.getPaymentDate()
            );
        }

        if (request.getNotes() != null) {
            payment.setNotes(
                    request.getNotes()
            );
        }

        recalculate(payment);

        addTimeline(
                payment,
                "Payment details updated",
                "edit",
                "#dbeafe",
                "#1d4ed8"
        );

        return paymentRepo.save(payment);
    }

    private boolean isLockedForTelecallerAmountEdits(
            String status
    ) {

        return Payment.STATUS_VERIFIED.equals(status)
                || Payment.STATUS_INVOICE_GENERATED.equals(status)
                || Payment.STATUS_INVOICE_SENT_TO_TELECALLER.equals(status)
                || Payment.STATUS_COMPLETED.equals(status)
                || Payment.STATUS_FULLY_PAID.equals(status);
    }

    /*
     * ============================================================
     * SEND PAYMENT REQUEST
     * ============================================================
     */

    public Payment recordPaymentDetailsSent(
            String id,
            String channel
    ) {

        Payment payment = get(id);

        payment.setLastSentChannel(channel);

        payment.setLastSentAt(
                Instant.now()
        );

        addTimeline(
                payment,
                "Payment details sent to client via " + channel,
                "WhatsApp".equalsIgnoreCase(channel)
                        ? "message"
                        : "mail",
                "#dcfce7",
                "#15803d"
        );

        return paymentRepo.save(payment);
    }

    /*
     * ============================================================
     * TELECALLER SENDS INVOICE TO CLIENT
     * ============================================================
     */

    public Payment sendInvoiceToClient(
            String id,
            String channel
    ) {

        Payment payment = get(id);

        if (payment.getInvoiceId() == null
                || payment.getInvoiceId().isBlank()) {

            throw new IllegalStateException(
                    "No invoice has been generated yet."
            );
        }

        if (!Payment.STATUS_INVOICE_SENT_TO_TELECALLER.equals(
                payment.getStatus()
        )
                && !Payment.STATUS_COMPLETED.equals(
                        payment.getStatus()
                )) {

            throw new IllegalStateException(
                    "Waiting for the Super Admin "
                            + "to send the invoice to you first."
            );
        }

        Invoice invoice = invoiceRepo
                .findById(payment.getInvoiceId())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Invoice record missing"
                        )
                );

        invoice.setSentToClientAt(
                Instant.now()
        );

        invoice.setSentToClientChannel(
                channel
        );

        invoiceRepo.save(invoice);

        payment.setInvoiceStatus(
                INVOICE_SENT_TO_CLIENT
        );

        /*
         * Keep Fully Paid status if the balance is already zero.
         * Otherwise workflow becomes Completed.
         */
        if (n(payment.getBalanceAmount()) <= 0
                && n(payment.getTotalAmount()) > 0) {

            payment.setStatus(
                    Payment.STATUS_FULLY_PAID
            );

        } else {

            payment.setStatus(
                    Payment.STATUS_COMPLETED
            );
        }

        addTimeline(
                payment,
                "Invoice sent to client via " + channel,
                "WhatsApp".equalsIgnoreCase(channel)
                        ? "message"
                        : "mail",
                "#dcfce7",
                "#15803d"
        );

        return paymentRepo.save(payment);
    }

    public File invoicePdfFileForTelecaller(
            String id
    ) {

        Payment payment = get(id);

        return invoiceFile(payment);
    }

    /*
     * ============================================================
     * SUPER ADMIN
     * ============================================================
     */

    public List<Payment> adminList() {
        return paymentRepo.findAll();
    }

    public Payment adminGet(String id) {

        return paymentRepo.findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Payment not found: " + id
                        )
                );
    }

    /*
     * ============================================================
     * SUPER ADMIN UPDATE PAYMENT
     * ============================================================
     */

    public Payment adminUpdate(
            String id,
            PaymentRequest request
    ) {

        Payment payment = adminGet(id);

        if (request.getOrganizationName() != null) {
            payment.setOrganizationName(
                    request.getOrganizationName()
            );
        }

        if (request.getAddress() != null) {
            payment.setAddress(
                    request.getAddress()
            );
        }

        if (request.getPhone() != null) {
            payment.setPhone(
                    request.getPhone()
            );
        }

        if (request.getEmail() != null) {
            payment.setEmail(
                    request.getEmail()
            );
        }

        if (request.getPaymentType() != null) {
            payment.setPaymentType(
                    request.getPaymentType()
            );
        }

        if (request.getTotalAmount() != null) {

            if (request.getTotalAmount()
                    < n(payment.getAmountPaid())) {

                throw new IllegalArgumentException(
                        "Total amount cannot be less than "
                                + "the amount already paid"
                );
            }

            payment.setTotalAmount(
                    request.getTotalAmount()
            );
        }

        if (request.getPaymentDate() != null) {
            payment.setPaymentDate(
                    request.getPaymentDate()
            );
        }

        if (request.getNotes() != null) {
            payment.setNotes(
                    request.getNotes()
            );
        }

        recalculate(payment);

        addTimeline(
                payment,
                "Payment details updated by Super Admin",
                "edit",
                "#dbeafe",
                "#1d4ed8"
        );

        return paymentRepo.save(payment);
    }

    /*
     * ============================================================
     * VERIFY PAYMENT - SUPER ADMIN
     * ============================================================
     */

    public Payment verify(String id) {

        Payment payment = adminGet(id);

        recalculate(payment);

        if (n(payment.getAmountPaid()) <= 0) {

            throw new IllegalStateException(
                    "Add at least one payment record "
                            + "before verification."
            );
        }

        payment.setVerificationStatus(
                VERIFICATION_VERIFIED
        );

        payment.setVerifiedBy(
                accessService.currentUsername()
        );

        payment.setVerifiedAt(
                Instant.now()
        );

        /*
         * Fully paid and verification are different concepts.
         *
         * A payment can be:
         * Fully Paid + Verified
         */
        if (n(payment.getBalanceAmount()) <= 0
                && n(payment.getTotalAmount()) > 0) {

            payment.setStatus(
                    Payment.STATUS_FULLY_PAID
            );

        } else {

            payment.setStatus(
                    Payment.STATUS_VERIFIED
            );
        }

        addTimeline(
                payment,
                "Payment verified by Super Admin",
                "check",
                "#dbeafe",
                "#1d4ed8"
        );

        paymentRepo.save(payment);

        /*
         * Verification now hands the invoice straight to the telecaller / admin dashboard:
         * generate the invoice and send it, so it shows up there immediately.
         * If this fails, verification still stands and the Super Admin can do both steps manually.
         */
        try {
            generateInvoice(id);
            sendInvoiceToTelecaller(id);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PaymentService.class)
                    .warn("Verified payment {} but auto invoice hand-off failed: {}",
                            id, e.getMessage());
        }

        return adminGet(id);
    }

    /*
     * ============================================================
     * GENERATE INVOICE - SUPER ADMIN
     * ============================================================
     */

    public Payment generateInvoice(String id) {

        Payment payment = adminGet(id);

        /*
         * Always recalculate before invoice generation.
         */
        recalculate(payment);

        /*
         * IMPORTANT:
         * Do not check only payment.getStatus() here.
         *
         * Fully Paid payment can still be Verified.
         */
        if (!VERIFICATION_VERIFIED.equalsIgnoreCase(
                payment.getVerificationStatus()
        )) {

            throw new IllegalStateException(
                    "Verify the payment before generating "
                            + "an invoice."
            );
        }

        Invoice invoice;

        if (payment.getInvoiceId() != null
                && !payment.getInvoiceId().isBlank()) {

            invoice = invoiceRepo
                    .findById(payment.getInvoiceId())
                    .orElseGet(Invoice::new);

        } else {

            invoice = new Invoice();
        }

        /*
         * ========================================================
         * PAYMENT / CLIENT DETAILS
         * ========================================================
         */

        invoice.setPaymentId(
                payment.getId()
        );

        invoice.setLeadId(
                payment.getLeadId()
        );

        if (invoice.getInvoiceNumber() == null
                || invoice.getInvoiceNumber().isBlank()) {

            invoice.setInvoiceNumber(
                    nextInvoiceNumber()
            );
        }

        invoice.setOrganizationName(firstNonBlank(invoice.getOrganizationName(), payment.getOrganizationName()));

        invoice.setAddress(firstNonBlank(invoice.getAddress(), payment.getAddress()));

        invoice.setPhone(firstNonBlank(invoice.getPhone(), payment.getPhone()));

        invoice.setEmail(firstNonBlank(invoice.getEmail(), payment.getEmail()));

        if (invoice.getInvoiceDate() == null
                || invoice.getInvoiceDate().isBlank()) {
            invoice.setInvoiceDate(
                    firstNonBlank(
                            payment.getPaymentDate(),
                            java.time.LocalDate.now().toString()
                    )
            );
        }

        if (invoice.getDescription() == null
                || invoice.getDescription().isBlank()) {
            invoice.setDescription(
                    firstNonBlank(
                            payment.getNotes(),
                            payment.getPaymentType() + " payment"
                    )
            );
        }

        /*
         * ========================================================
         * PAYMENT AMOUNTS
         * ========================================================
         */

        invoice.setTotalAmount(
                n(payment.getTotalAmount())
        );

        invoice.setAmountPaid(
                n(payment.getAmountPaid())
        );

        invoice.setBalanceAmount(
                n(payment.getBalanceAmount())
        );

        /*
         * Re-generating re-syncs the invoice with the payment records, so
         * manual line items are dropped (text edits are kept).
         */
        invoice.setItems(null);
        if (n(invoice.getDiscountAmount()) > n(payment.getTotalAmount())) {
            invoice.setDiscountAmount(0.0);
        }

        invoice.setPaymentType(
                payment.getPaymentType()
        );

        invoice.setPaymentDate(
                payment.getPaymentDate()
        );

        /*
         * Payment Status for Invoice:
         *
         * Fully Paid if balance = 0
         * Otherwise payment is Verified
         */
        String invoicePaymentStatus;

        if (n(payment.getBalanceAmount()) <= 0
                && n(payment.getTotalAmount()) > 0) {

            invoicePaymentStatus =
                    Payment.STATUS_FULLY_PAID;

        } else {

            invoicePaymentStatus =
                    payment.getVerificationStatus() != null
                            ? payment.getVerificationStatus()
                            : payment.getStatus();
        }

        invoice.setPaymentStatus(
                invoicePaymentStatus
        );

        /*
         * ========================================================
         * PAYMENT HISTORY SNAPSHOT
         * ========================================================
         */

        invoice.setPaymentHistory(
                payment.getPaymentHistory() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(
                                payment.getPaymentHistory()
                        )
        );

        /*
         * ========================================================
         * GENERATION AUDIT
         * ========================================================
         */

        invoice.setGeneratedBy(
                accessService.currentUsername()
        );

        invoice.setGeneratedAt(
                Instant.now()
        );

        /*
         * Save first so the invoice has a database record.
         */
        invoice = invoiceRepo.save(invoice);

        /*
         * ========================================================
         * GENERATE EXACT EXCEL TEMPLATE INVOICE
         * ========================================================
         */

        try {

            String excelPath =
                    excelService.generate(
                            invoice,
                            payment
                    );

            invoice.setExcelPath(
                    excelPath
            );

            invoice.setExcelFileName(
                    invoice.getInvoiceNumber()
                            + ".xlsx"
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to generate Excel invoice: "
                            + e.getMessage(),
                    e
            );
        }

        /*
         * ========================================================
         * GENERATE PDF
         * ========================================================
         */

        try {

            String pdfPath =
                    pdfService.generate(
                            invoice
                    );

            invoice.setPdfPath(
                    pdfPath
            );

            invoice.setPdfFileName(
                    invoice.getInvoiceNumber()
                            + ".pdf"
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to generate PDF invoice: "
                            + e.getMessage(),
                    e
            );
        }

        /*
         * Save generated paths.
         */
        invoice = invoiceRepo.save(invoice);

        /*
         * ========================================================
         * UPDATE PAYMENT
         * ========================================================
         */

        payment.setInvoiceId(
                invoice.getId()
        );

        payment.setInvoiceStatus(
                INVOICE_GENERATED
        );

        /*
         * Preserve Fully Paid.
         */
        if (n(payment.getBalanceAmount()) <= 0
                && n(payment.getTotalAmount()) > 0) {

            payment.setStatus(
                    Payment.STATUS_FULLY_PAID
            );

        } else {

            payment.setStatus(
                    Payment.STATUS_INVOICE_GENERATED
            );
        }

        addTimeline(
                payment,
                "Invoice "
                        + invoice.getInvoiceNumber()
                        + " generated by Super Admin",
                "document",
                "#dbeafe",
                "#1d4ed8"
        );

        return paymentRepo.save(payment);
    }

    /*
     * ============================================================
     * SEND INVOICE TO TELECALLER
     * ============================================================
     */

    public Payment sendInvoiceToTelecaller(
            String id
    ) {

        Payment payment = adminGet(id);

        if (payment.getInvoiceId() == null
                || payment.getInvoiceId().isBlank()) {

            throw new IllegalStateException(
                    "Generate the invoice before sending "
                            + "it to the telecaller."
            );
        }

        Invoice invoice = invoiceRepo
                .findById(payment.getInvoiceId())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Invoice record missing"
                        )
                );

        invoice.setSentToTelecallerAt(
                Instant.now()
        );

        invoiceRepo.save(invoice);

        payment.setInvoiceStatus(
                INVOICE_SENT_TO_TELECALLER
        );

        /*
         * This workflow status indicates the invoice
         * is now available to the Telecaller.
         */
        payment.setStatus(
                Payment.STATUS_INVOICE_SENT_TO_TELECALLER
        );

        addTimeline(
                payment,
                "Invoice sent to telecaller "
                        + nz(payment.getTelecallerName()),
                "send",
                "#e0e7ff",
                "#4338ca"
        );

        return paymentRepo.save(payment);
    }

    /*
     * ============================================================
     * INVOICE DOWNLOAD
     * ============================================================
     */

    public File invoicePdfFileForAdmin(
            String id
    ) {

        Payment payment = adminGet(id);

        return invoiceFile(payment);
    }

    public File invoiceExcelFileForAdmin(
            String id
    ) {

        Payment payment = adminGet(id);

        return invoiceExcelFile(payment);
    }

    public File invoiceExcelFileForTelecaller(
            String id
    ) {

        Payment payment = get(id);

        return invoiceExcelFile(payment);
    }

    public Invoice getInvoice(
            String paymentId
    ) {

        Payment payment =
                adminGet(paymentId);

        if (payment.getInvoiceId() == null
                || payment.getInvoiceId().isBlank()) {

            throw new IllegalArgumentException(
                    "No invoice generated yet"
            );
        }

        return invoiceRepo
                .findById(payment.getInvoiceId())
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Invoice record missing"
                        )
                );
    }

    /*
     * ============================================================
     * TELECALLER READS THE INVOICE THE SUPER ADMIN GENERATED / EDITED
     * ============================================================
     */

    public Invoice invoiceForTelecaller(String paymentId) {
        Payment payment = get(paymentId); // also checks the payment belongs to this telecaller

        if (payment.getInvoiceId() == null
                || payment.getInvoiceId().isBlank()) {
            throw new IllegalArgumentException(
                    "No invoice has been generated yet"
            );
        }

        return invoiceRepo
                .findById(payment.getInvoiceId())
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Invoice record missing"
                        )
                );
    }

    /*
     * ============================================================
     * EDIT GENERATED INVOICE - SUPER ADMIN
     * ============================================================
     */

    private static final Pattern INVOICE_NO =
            Pattern.compile("^INV-IC/\\d{5,}$");

    /** Super Admin edits any invoice. */
    public Invoice updateInvoice(
            String paymentId,
            InvoiceUpdateRequest r
    ) {
        return applyInvoiceUpdate(
                adminGet(paymentId),
                getInvoice(paymentId),
                r,
                "Super Admin"
        );
    }

    /** Telecaller edits the invoice of one of THEIR OWN payments. */
    public Invoice updateInvoiceForTelecaller(
            String paymentId,
            InvoiceUpdateRequest r
    ) {
        Payment payment = get(paymentId); // ownership check
        return applyInvoiceUpdate(
                payment,
                invoiceForTelecaller(paymentId),
                r,
                "Telecaller"
        );
    }

    private Invoice applyInvoiceUpdate(
            Payment payment,
            Invoice invoice,
            InvoiceUpdateRequest r,
            String editedByRole
    ) {

        /* ---------- invoice number (format + unique) ---------- */
        if (r.getInvoiceNumber() != null
                && !r.getInvoiceNumber().isBlank()) {

            String number = r.getInvoiceNumber().trim();

            if (!INVOICE_NO.matcher(number).matches()) {
                throw new IllegalArgumentException(
                        "Invoice number format must be INV-IC/00760"
                );
            }

            if (!number.equals(invoice.getInvoiceNumber())) {
                final String currentId = invoice.getId();
                invoiceRepo.findByInvoiceNumber(number)
                        .filter(other -> !other.getId().equals(currentId))
                        .ifPresent(other -> {
                            throw new IllegalArgumentException(
                                    "Invoice number " + number
                                            + " is already used"
                            );
                        });
                invoice.setInvoiceNumber(number);
            }
        }

        if (r.getInvoiceDate() != null && !r.getInvoiceDate().isBlank())
            invoice.setInvoiceDate(r.getInvoiceDate().trim());
        if (r.getPaymentStatus() != null && !r.getPaymentStatus().isBlank())
            invoice.setPaymentStatus(r.getPaymentStatus().trim());

        /* ---------- plain text fields (client, company, bank, signature...) ---------- */
        if (r.getOrganizationName() != null) invoice.setOrganizationName(r.getOrganizationName().trim());
        if (r.getAddress() != null) invoice.setAddress(r.getAddress().trim());
        if (r.getPhone() != null) invoice.setPhone(r.getPhone().trim());
        if (r.getCustomerGstin() != null) {
            String gstin = r.getCustomerGstin().trim().toUpperCase();
            if (!gstin.isEmpty()
                    && !gstin.matches("^\\d{2}[A-Z]{5}\\d{4}[A-Z][A-Z\\d]Z[A-Z\\d]$"))
                throw new IllegalArgumentException(
                        "GSTIN must be a valid 15-character GST number");
            invoice.setCustomerGstin(gstin);
        }
        if (r.getEmail() != null) invoice.setEmail(r.getEmail().trim());
        if (r.getPaymentType() != null) invoice.setPaymentType(r.getPaymentType().trim());
        if (r.getShippingName() != null) invoice.setShippingName(r.getShippingName().trim());
        if (r.getShippingAddress() != null) invoice.setShippingAddress(r.getShippingAddress().trim());
        if (r.getCompanyName() != null) invoice.setCompanyName(r.getCompanyName().trim());
        if (r.getCompanyAddress() != null) invoice.setCompanyAddress(r.getCompanyAddress().trim());
        if (r.getCompanyEmail() != null) invoice.setCompanyEmail(r.getCompanyEmail().trim());
        if (r.getCompanyMobile() != null) invoice.setCompanyMobile(r.getCompanyMobile().trim());
        if (r.getCompanyWebsite() != null) invoice.setCompanyWebsite(r.getCompanyWebsite().trim());
        if (r.getBankName() != null) invoice.setBankName(r.getBankName().trim());
        if (r.getBankBranch() != null) invoice.setBankBranch(r.getBankBranch().trim());
        if (r.getBankIfsc() != null) invoice.setBankIfsc(r.getBankIfsc().trim());
        if (r.getBankAccountNo() != null) invoice.setBankAccountNo(r.getBankAccountNo().trim());
        if (r.getDeclaration() != null) invoice.setDeclaration(r.getDeclaration().trim());
        if (r.getSignatoryFor() != null) invoice.setSignatoryFor(r.getSignatoryFor().trim());
        if (r.getSignatoryName() != null) invoice.setSignatoryName(r.getSignatoryName().trim());
        if (r.getSignatoryTitle() != null) invoice.setSignatoryTitle(r.getSignatoryTitle().trim());

        /* ---------- line items -> subtotal ---------- */
        if (r.getItems() != null) {
            if (r.getItems().isEmpty())
                throw new IllegalArgumentException("Add at least one invoice item");
            if (r.getItems().size() > 50)
                throw new IllegalArgumentException("An invoice can have at most 50 items");

            List<Invoice.Item> clean = new ArrayList<>();
            double sum = 0;
            for (Invoice.Item in : r.getItems()) {
                if (in.getDescription() == null || in.getDescription().isBlank())
                    throw new IllegalArgumentException("Every item needs a description");
                if (in.getQuantity() == null || in.getQuantity() <= 0)
                    throw new IllegalArgumentException("Item quantity must be greater than zero");
                if (in.getRate() == null || in.getRate() < 0)
                    throw new IllegalArgumentException("Item rate cannot be negative");
                Invoice.Item item = new Invoice.Item();
                item.setDescription(in.getDescription().trim());
                item.setQuantity(in.getQuantity());
                item.setRate(in.getRate());
                clean.add(item);
                sum += in.getQuantity() * in.getRate();
            }
            invoice.setItems(clean);
            invoice.setTotalAmount(Math.round(sum * 100.0) / 100.0);
            invoice.setDescription(clean.get(0).getDescription());
        }

        /* ---------- discount / tax / paid ---------- */
        double total = n(invoice.getTotalAmount());

        if (r.getDiscountAmount() != null) {
            if (r.getDiscountAmount() < 0)
                throw new IllegalArgumentException("Discount cannot be negative");
            invoice.setDiscountAmount(r.getDiscountAmount());
        }
        if (n(invoice.getDiscountAmount()) > total)
            throw new IllegalArgumentException("Discount cannot be more than the subtotal");

        if (r.getTaxRate() != null) {
            if (r.getTaxRate() < 0 || r.getTaxRate() > 100)
                throw new IllegalArgumentException("Tax rate must be between 0 and 100");
            invoice.setTaxRate(r.getTaxRate());
        }

        if (r.getAmountPaid() != null) {
            if (r.getAmountPaid() < 0)
                throw new IllegalArgumentException("Paid amount cannot be negative");
            invoice.setAmountPaid(r.getAmountPaid());
        }

        double taxable = Math.max(0, total - n(invoice.getDiscountAmount()));
        double grand = taxable + (taxable * n(invoice.getTaxRate())) / 100.0;
        if (n(invoice.getAmountPaid()) > grand + 0.005)
            throw new IllegalArgumentException("Paid amount cannot be more than the invoice total");
        invoice.setBalanceAmount(Math.max(0, Math.round((grand - n(invoice.getAmountPaid())) * 100.0) / 100.0));

        invoice.setEditedBy(accessService.currentUsername());
        invoice.setEditedAt(Instant.now());
        invoice = invoiceRepo.save(invoice);

        /* keep Telecaller PDF / Excel downloads in sync with the edit */
        try {
            invoice.setExcelPath(excelService.generate(invoice, payment));
            invoice.setPdfPath(pdfService.generate(invoice));
            invoice = invoiceRepo.save(invoice);
        } catch (Exception e) {
            // The edit is already saved. PDF / Excel are rebuilt in the browser from the saved data,
            // so a failed server-side file must not make the whole Save look failed.
            org.slf4j.LoggerFactory.getLogger(PaymentService.class)
                    .warn("Invoice {} saved, but PDF/Excel regeneration failed: {}",
                            invoice.getInvoiceNumber(), e.getMessage());
        }

        addTimeline(
                payment,
                "Invoice " + invoice.getInvoiceNumber()
                        + " edited by " + editedByRole,
                "edit",
                "#dbeafe",
                "#1d4ed8"
        );
        paymentRepo.save(payment);

        return invoice;
    }

    /*
     * ============================================================
     * ADD PAYMENT RECORD
     * ============================================================
     */

    public Payment addPaymentRecord(
            String id,
            PaymentRecordRequest request
    ) {

        Payment payment = get(id);

        return addRecordInternal(
                payment,
                request
        );
    }

    public Payment adminAddPaymentRecord(
            String id,
            PaymentRecordRequest request
    ) {

        Payment payment = adminGet(id);

        return addRecordInternal(
                payment,
                request
        );
    }

    private Payment addRecordInternal(
            Payment payment,
            PaymentRecordRequest request
    ) {

        recalculate(payment);

        if (n(payment.getBalanceAmount()) <= 0
                || Payment.STATUS_FULLY_PAID.equals(
                        payment.getStatus()
                )) {

            throw new IllegalStateException(
                    "The client is already fully paid. "
                            + "No more payment records can be added "
                            + "unless the Super Admin increases "
                            + "the total amount."
            );
        }

        double amount =
                n(request.getPaymentAmount());

        if (amount <= 0) {

            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero"
            );
        }

        if (amount > n(payment.getBalanceAmount())) {

            throw new IllegalArgumentException(
                    "Payment amount cannot exceed "
                            + "remaining balance"
            );
        }

        Payment.PaymentRecord record =
                new Payment.PaymentRecord();

        record.setId(
                UUID.randomUUID().toString()
        );

        record.setAmount(
                amount
        );

        record.setPaymentType(
                request.getPaymentType()
        );

        record.setPaymentDate(
                request.getPaymentDate()
        );

        record.setPaymentMethod(
                request.getPaymentMethod()
        );

        record.setTransactionReferenceNumber(
                request.getTransactionReferenceNumber()
        );

        record.setNotes(
                request.getNotes()
        );

        record.setStatus(
                firstNonBlank(
                        request.getStatus(),
                        "Paid"
                )
        );

        record.setRecordedBy(
                accessService.currentUsername()
        );

        record.setRecordedAt(
                Instant.now()
        );

        if (payment.getPaymentHistory() == null) {

            payment.setPaymentHistory(
                    new ArrayList<>()
            );
        }

        payment.getPaymentHistory().add(
                record
        );

        recalculate(payment);

        /*
         * Payment changed after verification.
         * It must be verified again.
         */
        resetVerificationAndInvoiceAfterPaymentChange(
                payment
        );

        addTimeline(
                payment,
                "Payment record added: "
                        + fmt(amount),
                "plus",
                "#dcfce7",
                "#15803d"
        );

        return paymentRepo.save(payment);
    }

    /*
     * ============================================================
     * UPDATE PAYMENT HISTORY RECORD
     * ============================================================
     */

    public Payment adminUpdatePaymentRecord(
            String id,
            String recordId,
            PaymentRecordRequest request
    ) {

        Payment payment = adminGet(id);

        if (payment.getPaymentHistory() == null) {

            throw new IllegalArgumentException(
                    "Payment history record not found"
            );
        }

        Payment.PaymentRecord record =
                payment.getPaymentHistory()
                        .stream()
                        .filter(
                                item -> recordId.equals(
                                        item.getId()
                                )
                        )
                        .findFirst()
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Payment history record not found"
                                )
                        );

        if (request.getPaymentAmount() != null) {

            if (request.getPaymentAmount() <= 0) {

                throw new IllegalArgumentException(
                        "Payment amount must be greater than zero"
                );
            }

            record.setAmount(
                    request.getPaymentAmount()
            );
        }

        if (request.getPaymentType() != null) {
            record.setPaymentType(
                    request.getPaymentType()
            );
        }

        if (request.getPaymentDate() != null) {
            record.setPaymentDate(
                    request.getPaymentDate()
            );
        }

        if (request.getPaymentMethod() != null) {
            record.setPaymentMethod(
                    request.getPaymentMethod()
            );
        }

        if (request.getTransactionReferenceNumber() != null) {
            record.setTransactionReferenceNumber(
                    request.getTransactionReferenceNumber()
            );
        }

        if (request.getNotes() != null) {
            record.setNotes(
                    request.getNotes()
            );
        }

        if (request.getStatus() != null) {
            record.setStatus(
                    request.getStatus()
            );
        }

        recalculate(payment);

        if (n(payment.getAmountPaid())
                > n(payment.getTotalAmount())) {

            throw new IllegalArgumentException(
                    "Total paid cannot exceed total amount"
            );
        }

        /*
         * Record changed.
         * Old verification and invoice are no longer valid.
         */
        resetVerificationAndInvoiceAfterPaymentChange(
                payment
        );

        addTimeline(
                payment,
                "Payment history record updated "
                        + "by Super Admin",
                "edit",
                "#dbeafe",
                "#1d4ed8"
        );

        return paymentRepo.save(payment);
    }

    /*
     * ============================================================
     * RESET AFTER PAYMENT CHANGE
     * ============================================================
     */

    private void resetVerificationAndInvoiceAfterPaymentChange(
            Payment payment
    ) {

        payment.setVerificationStatus(
                VERIFICATION_PENDING
        );

        payment.setVerifiedBy(null);

        payment.setVerifiedAt(null);

        payment.setInvoiceStatus(
                INVOICE_NOT_GENERATED
        );

        /*
         * Keep invoiceId if historical invoice exists,
         * but current payment must be re-verified.
         */
        if (n(payment.getBalanceAmount()) <= 0
                && n(payment.getTotalAmount()) > 0) {

            payment.setStatus(
                    Payment.STATUS_FULLY_PAID
            );

        } else {

            payment.setStatus(
                    Payment.STATUS_RECORDED
            );
        }
    }

    /*
     * ============================================================
     * INITIAL PAYMENT REQUEST -> PAYMENT HISTORY RECORD
     * ============================================================
     */

    private Payment.PaymentRecord toRecord(
            PaymentRequest request,
            String user
    ) {

        Payment.PaymentRecord record =
                new Payment.PaymentRecord();

        record.setId(
                UUID.randomUUID().toString()
        );

        record.setAmount(
                n(request.getAmountPaid())
        );

        record.setPaymentType(
                request.getPaymentType()
        );

        record.setPaymentDate(
                request.getPaymentDate()
        );

        record.setPaymentMethod(
                request.getPaymentMethod()
        );

        record.setTransactionReferenceNumber(
                request.getTransactionReferenceNumber()
        );

        record.setNotes(
                request.getNotes()
        );

        record.setStatus(
                firstNonBlank(
                        request.getPaymentStatus(),
                        "Paid"
                )
        );

        record.setRecordedBy(
                user
        );

        record.setRecordedAt(
                Instant.now()
        );

        return record;
    }

    /*
     * ============================================================
     * BALANCE CALCULATION
     * ============================================================
     */

    private void recalculate(
            Payment payment
    ) {

        double totalPaid = 0.0;

        if (payment.getPaymentHistory() != null) {

            totalPaid =
                    payment.getPaymentHistory()
                            .stream()
                            .filter(
                                    record ->
                                            !"Pending".equalsIgnoreCase(
                                                    record.getStatus()
                                            )
                                                    && !"Remaining".equalsIgnoreCase(
                                                            record.getStatus()
                                                    )
                            )
                            .map(
                                    Payment.PaymentRecord::getAmount
                            )
                            .filter(
                                    java.util.Objects::nonNull
                            )
                            .mapToDouble(
                                    Double::doubleValue
                            )
                            .sum();
        }

        payment.setAmountPaid(
                totalPaid
        );

        double balance =
                Math.max(
                        0,
                        n(payment.getTotalAmount())
                                - totalPaid
                );

        payment.setBalanceAmount(
                balance
        );

        /*
         * Only derive collection status here.
         * Do not overwrite workflow statuses unnecessarily.
         */
        if (balance <= 0
                && n(payment.getTotalAmount()) > 0) {

            payment.setStatus(
                    Payment.STATUS_FULLY_PAID
            );

        } else if (Payment.STATUS_FULLY_PAID.equals(
                payment.getStatus()
        )) {

            if (VERIFICATION_VERIFIED.equalsIgnoreCase(
                    payment.getVerificationStatus()
            )) {

                payment.setStatus(
                        Payment.STATUS_VERIFIED
                );

            } else {

                payment.setStatus(
                        Payment.STATUS_RECORDED
                );
            }
        }
    }

    /*
     * ============================================================
     * SUPER ADMIN QR CODE
     * ============================================================
     */

    public PaymentQrCode uploadQr(
            MultipartFile file
    ) throws Exception {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "QR code image is required"
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null
                || !contentType.startsWith(
                        "image/"
                )) {

            throw new IllegalArgumentException(
                    "Only image QR files are allowed"
            );
        }

        /*
         * Only one active Super Admin QR.
         */
        qrRepo.deleteAll();

        PaymentQrCode qr =
                new PaymentQrCode();

        qr.setFileName(
                file.getOriginalFilename()
        );

        qr.setContentType(
                contentType
        );

        qr.setData(
                file.getBytes()
        );

        qr.setUploadedAt(
                Instant.now()
        );

        qr.setUploadedBy(
                accessService.currentUsername()
        );

        return qrRepo.save(qr);
    }

    public PaymentQrCode currentQr() {

        return qrRepo.findAll()
                .stream()
                .findFirst()
                .orElse(null);
    }

    /*
     * ============================================================
     * INVOICE FILE HELPERS
     * ============================================================
     */

    private File invoiceFile(
            Payment payment
    ) {

        if (payment.getInvoiceId() == null
                || payment.getInvoiceId().isBlank()) {

            throw new IllegalStateException(
                    "No invoice generated for this payment yet."
            );
        }

        Invoice invoice =
                invoiceRepo
                        .findById(
                                payment.getInvoiceId()
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Invoice record missing"
                                )
                        );

        if (invoice.getPdfPath() == null
                || invoice.getPdfPath().isBlank()) {

            throw new IllegalStateException(
                    "PDF invoice file is not available."
            );
        }

        return new File(
                invoice.getPdfPath()
        );
    }

    private File invoiceExcelFile(
            Payment payment
    ) {

        if (payment.getInvoiceId() == null
                || payment.getInvoiceId().isBlank()) {

            throw new IllegalStateException(
                    "No invoice generated for this payment yet."
            );
        }

        Invoice invoice =
                invoiceRepo
                        .findById(
                                payment.getInvoiceId()
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Invoice record missing"
                                )
                        );

        if (invoice.getExcelPath() == null
                || invoice.getExcelPath().isBlank()) {

            throw new IllegalStateException(
                    "Exact Excel invoice file "
                            + "is not available."
            );
        }

        return new File(
                invoice.getExcelPath()
        );
    }

    /*
     * ============================================================
     * HELPERS
     * ============================================================
     */

    private String nextInvoiceNumber() {
        // INV-IC/00760, INV-IC/00761, ... (atomic counter, never duplicates)
        return invoiceNumberService.next();
    }

    private void addTimeline(
            Payment payment,
            String action,
            String icon,
            String bg,
            String color
    ) {

        if (payment.getTimeline() == null) {

            payment.setTimeline(
                    new ArrayList<>()
            );
        }

        payment.getTimeline()
                .add(
                        new Payment.TimelineEntry(
                                action,
                                TS_FMT.format(
                                        Instant.now()
                                ),
                                icon,
                                bg,
                                color
                        )
                );
    }

    private String fmt(
            Double value
    ) {

        return "₹"
                + String.format(
                        "%,.0f",
                        value == null
                                ? 0
                                : value
                );
    }

    private double n(
            Double value
    ) {

        return value == null
                ? 0.0
                : value;
    }

    private String nz(
            String value
    ) {

        return value == null
                ? "-"
                : value;
    }

    private String firstNonBlank(
            String... values
    ) {

        for (String value : values) {

            if (value != null
                    && !value.isBlank()) {

                return value;
            }
        }

        return null;
    }
}