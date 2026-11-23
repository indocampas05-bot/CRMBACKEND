package com.indocrm.indocrm.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.model.Invoice;

@Service
public class InvoicePdfService {

    @Value("${invoice.storage.dir:./storage/invoices}")
    private String storageDirProp;

    /*
     * IMPORTANT:
     * Do not use NumberFormat.getCurrencyInstance(new Locale("en", "IN"))
     * here because it produces the ₹ symbol.
     *
     * PDFBox built-in Helvetica uses WinAnsiEncoding and does not support ₹.
     */
    private static final NumberFormat NUMBER_FORMAT =
            NumberFormat.getNumberInstance(Locale.US);

    private Path outputDir() throws IOException {
        Path dir = Paths.get(storageDirProp, "generated");
        Files.createDirectories(dir);
        return dir;
    }

    public String generate(Invoice invoice) throws IOException {

        String fileName = invoice.getInvoiceNumber() != null
                ? invoice.getInvoiceNumber()
                : invoice.getId();

        Path outPath = outputDir()
                .resolve(safeName(fileName) + ".pdf");

        try (PDDocument doc = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float margin = 50;
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            float width = pageWidth - (2 * margin);
            float y = pageHeight - margin;

            try (PDPageContentStream cs =
                         new PDPageContentStream(doc, page)) {

                /*
                 * ============================================================
                 * HEADER BACKGROUND
                 * ============================================================
                 */

                cs.setNonStrokingColor(123, 17, 19);

                cs.addRect(
                        margin,
                        y - 40,
                        width,
                        40
                );

                cs.fill();

                /*
                 * ============================================================
                 * HEADER TEXT
                 * ============================================================
                 */

                cs.setNonStrokingColor(255, 255, 255);

                writeText(
                        cs,
                        PDType1Font.HELVETICA_BOLD,
                        18,
                        margin + 12,
                        y - 27,
                        "INVOICE"
                );

                writeText(
                        cs,
                        PDType1Font.HELVETICA_BOLD,
                        12,
                        margin + width - 160,
                        y - 27,
                        nz(invoice.getInvoiceNumber())
                );

                /*
                 * ============================================================
                 * CONTENT COLOR
                 * ============================================================
                 */

                cs.setNonStrokingColor(0, 0, 0);

                y -= 70;

                /*
                 * ============================================================
                 * INVOICE DETAILS
                 * ============================================================
                 */

                y = writeLabelValue(
                        cs,
                        margin,
                        y,
                        "Invoice Date",
                        nz(invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : invoice.getPaymentDate())
                );

                y -= 20;

                /*
                 * ============================================================
                 * BILL TO
                 * ============================================================
                 */

                writeText(
                        cs,
                        PDType1Font.HELVETICA_BOLD,
                        11,
                        margin,
                        y,
                        "Bill To"
                );

                y -= 16;

                y = writeLabelValue(
                        cs,
                        margin,
                        y,
                        "Organization",
                        nz(invoice.getOrganizationName())
                );

                y = writeLabelValue(
                        cs,
                        margin,
                        y,
                        "Address",
                        nz(invoice.getAddress())
                );

                y = writeLabelValue(
                        cs,
                        margin,
                        y,
                        "Phone",
                        nz(invoice.getPhone())
                );

                y = writeLabelValue(
                        cs,
                        margin,
                        y,
                        "Email",
                        nz(invoice.getEmail())
                );

                /*
                 * ============================================================
                 * PAYMENT SUMMARY
                 * ============================================================
                 */

                y -= 20;

                writeText(
                        cs,
                        PDType1Font.HELVETICA_BOLD,
                        11,
                        margin,
                        y,
                        "Payment Summary"
                );

                y -= 16;

                y = writeLabelValue(
                        cs,
                        margin,
                        y,
                        "Payment Type",
                        nz(invoice.getPaymentType())
                );

                y = writeLabelValue(
                        cs,
                        margin,
                        y,
                        "Total Amount",
                        fmt(invoice.getTotalAmount())
                );

                y = writeLabelValue(
                        cs,
                        margin,
                        y,
                        "Amount Paid",
                        fmt(invoice.getAmountPaid())
                );

                y = writeLabelValue(
                        cs,
                        margin,
                        y,
                        "Balance Amount",
                        fmt(invoice.getBalanceAmount())
                );

                y = writeLabelValue(cs, margin, y, "Payment Status", nz(invoice.getPaymentStatus()));

                if (invoice.getPaymentHistory() != null && !invoice.getPaymentHistory().isEmpty()) {
                    y -= 18;
                    writeText(cs, PDType1Font.HELVETICA_BOLD, 11, margin, y, "Payment History");
                    y -= 16;
                    for (int i = 0; i < invoice.getPaymentHistory().size() && y > 90; i++) {
                        var r = invoice.getPaymentHistory().get(i);
                        String row = "Payment " + (i + 1) + " | " + fmt(r.getAmount()) + " | " + nz(r.getPaymentDate())
                                + " | " + nz(r.getPaymentType()) + " | " + nz(r.getPaymentMethod())
                                + " | Ref: " + nz(r.getTransactionReferenceNumber()) + " | " + nz(r.getStatus());
                        writeText(cs, PDType1Font.HELVETICA, 8, margin, y, row);
                        y -= 13;
                    }
                }

                /*
                 * ============================================================
                 * FOOTER LINE
                 * ============================================================
                 */

                y -= 30;

                cs.setLineWidth(0.5f);

                cs.moveTo(margin, y);
                cs.lineTo(margin + width, y);

                cs.stroke();

                y -= 20;

                /*
                 * ============================================================
                 * FOOTER TEXT
                 * ============================================================
                 */

                writeText(
                        cs,
                        PDType1Font.HELVETICA_OBLIQUE,
                        9,
                        margin,
                        y,
                        "This is a system-generated invoice from IndoCRM."
                );
            }

            doc.save(outPath.toFile());
        }

        return outPath
                .toAbsolutePath()
                .toString();
    }

    /*
     * ============================================================
     * LABEL + VALUE
     * ============================================================
     */

    private float writeLabelValue(
            PDPageContentStream cs,
            float x,
            float y,
            String label,
            String value
    ) throws IOException {

        writeText(
                cs,
                PDType1Font.HELVETICA_BOLD,
                10,
                x,
                y,
                label + ":"
        );

        writeText(
                cs,
                PDType1Font.HELVETICA,
                10,
                x + 140,
                y,
                value
        );

        return y - 16;
    }

    /*
     * ============================================================
     * WRITE TEXT
     * ============================================================
     */

    private void writeText(
            PDPageContentStream cs,
            PDType1Font font,
            float size,
            float x,
            float y,
            String text
    ) throws IOException {

        cs.beginText();

        cs.setFont(font, size);

        cs.newLineAtOffset(x, y);

        cs.showText(
                sanitize(text)
        );

        cs.endText();
    }

    /*
     * ============================================================
     * AMOUNT FORMAT
     * ============================================================
     *
     * Example:
     *
     * 10000     -> Rs. 10,000.00
     * 2500.50   -> Rs. 2,500.50
     *
     * We intentionally use "Rs." instead of "₹" because Helvetica
     * does not support the Unicode Rupee symbol.
     *
     * ============================================================
     */

    private String fmt(Double value) {

        double amount = value == null
                ? 0.0
                : value;

        NUMBER_FORMAT.setMinimumFractionDigits(2);
        NUMBER_FORMAT.setMaximumFractionDigits(2);

        return "Rs. " + NUMBER_FORMAT.format(amount);
    }

    /*
     * ============================================================
     * NULL SAFE
     * ============================================================
     */

    private String nz(String value) {

        if (value == null || value.isBlank()) {
            return "-";
        }

        return value;
    }

    /*
     * ============================================================
     * SAFE FILE NAME
     * ============================================================
     */

    private String safeName(String value) {

        if (value == null || value.isBlank()) {
            return "invoice";
        }

        return value.replaceAll(
                "[^a-zA-Z0-9-_]",
                "_"
        );
    }

    /*
     * ============================================================
     * HELVETICA SAFE TEXT
     * ============================================================
     *
     * Prevent unsupported characters from crashing PDF generation.
     *
     * ============================================================
     */

    private String sanitize(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("₹", "Rs.")
                .replace("–", "-")
                .replace("—", "-")
                .replace("•", "-")
                .replace("…", "...");
    }
}