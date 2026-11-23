package com.indocrm.indocrm.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.model.Invoice;
import com.indocrm.indocrm.model.Payment;

/**
 * Generates an invoice by opening the supplied DKT INVOICE.xlsx master file
 * and changing values only. Row heights, column widths, merged cells, fonts,
 * borders, fills, formulas, alignment and the complete design are retained.
 */
@Service
public class InvoiceExcelService {

    private static final String TEMPLATE = "invoice-templates/DKT INVOICE.xlsx";

    @Value("${invoice.storage.dir:./storage/invoices}")
    private String storageDirProp;

    public String generate(Invoice invoice, Payment payment) throws IOException {
        Path outputDir = Paths.get(storageDirProp, "generated");
        Files.createDirectories(outputDir);

        String safeNumber = safeName(invoice.getInvoiceNumber());
        Path output = outputDir.resolve(safeNumber + ".xlsx");

        ClassPathResource resource = new ClassPathResource(TEMPLATE);
        if (!resource.exists()) {
            throw new IOException("Invoice Excel master template is missing: " + TEMPLATE);
        }

        try (InputStream in = resource.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {

            Sheet sheet = workbook.getSheet("DKT");
            if (sheet == null) sheet = workbook.getSheetAt(0);

          String billTo = joinClient(
        firstNonBlank(invoice.getOrganizationName(), payment.getOrganizationName()),
        firstNonBlank(invoice.getAddress(), payment.getAddress()));
if (invoice.getPhone() != null && !invoice.getPhone().isBlank())
    billTo += "\nContact: " + invoice.getPhone();
if (invoice.getCustomerGstin() != null && !invoice.getCustomerGstin().isBlank())
    billTo += "\nGSTIN: " + invoice.getCustomerGstin();
            String shipTo = joinClient(
                    firstNonBlank(invoice.getShippingName(), invoice.getOrganizationName(), payment.getOrganizationName()),
                    firstNonBlank(invoice.getShippingAddress(), invoice.getAddress(), payment.getAddress()));
            String invoiceDate = firstNonBlank(
                    formatPaymentDate(invoice.getInvoiceDate()),
                    formatPaymentDate(payment.getPaymentDate()),
                    DateTimeFormatter.ofPattern("dd-MM-yyyy")
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.now()));

            // Master-template fields. Existing formatting and merged cells are untouched.
            setString(sheet, 8, 2, billTo);   // B9
            setString(sheet, 8, 6, shipTo);   // F9 Shipping Address
            setString(sheet, 7, 10, invoice.getInvoiceNumber()); // J8
            setString(sheet, 8, 10, invoiceDate);                 // J9
            String statusText = firstNonBlank(invoice.getPaymentStatus(), paymentStatus(payment));
            if ("Fully Paid".equalsIgnoreCase(statusText)) statusText = "Paid";
            setString(sheet, 9, 10, statusText);                  // J10

            String description = firstNonBlank(
                    invoice.getDescription(),
                    payment.getNotes(),
                    payment.getPaymentType() + " payment");
            double itemQty = 1d;
            double itemRate = n(invoice.getTotalAmount());
            java.util.List<com.indocrm.indocrm.model.Invoice.Item> items = invoice.getItems();
            if (items != null && items.size() == 1) {
                description = items.get(0).getDescription();
                itemQty = items.get(0).getQuantity() == null ? 1d : items.get(0).getQuantity();
                itemRate = items.get(0).getRate() == null ? 0d : items.get(0).getRate();
            } else if (items != null && items.size() > 1) {
                // the master template has a single item row -> combine
                StringBuilder sb = new StringBuilder();
                for (var it : items) { if (sb.length() > 0) sb.append("; "); sb.append(it.getDescription()); }
                description = sb.toString();
            }
            setString(sheet, 15, 3, description);                 // C16
            setNumber(sheet, 15, 8, itemQty);                    // H16 Qty
            setNumber(sheet, 15, 9, itemRate);                   // I16 Rate
            setFormula(sheet, 15, 11, "H16*I16");                // K16 Amount

            setString(sheet, 16, 3, firstNonBlank(invoice.getPaymentType(), payment.getPaymentType()) + " Received"); // C17
            setNumber(sheet, 16, 8, 1d);                          // H17 Qty
            setNumber(sheet, 16, 9, n(invoice.getAmountPaid()));   // I17
            setFormula(sheet, 16, 11, "I17");                    // K17 Advance Received

            // Preserve the supplied template's calculation design.
            setFormula(sheet, 17, 11, "SUM(K16:K16)");
            setNumber(sheet, 18, 11, n(invoice.getDiscountAmount())); // K19 Discount
            setNumber(sheet, 20, 11, n(invoice.getTaxRate()));        // K21 Tax rate %
            setFormula(sheet, 19, 11, "K18-K19");
            setFormula(sheet, 21, 11, "K20*K21/100");
            setFormula(sheet, 22, 11, "K20+K22-K17");
            setFormula(sheet, 23, 11, "K23");

            // Keep the exact supplied bank/invoice design; only dynamic values are changed.
            String words = amountInWords(n(invoice.getAmountPaid()));
            if (words != null && !words.isBlank()) {
                setString(sheet, 24, 2, "IN WORDS: " + words + " only");
            }

            workbook.write(Files.newOutputStream(output));
        }

        return output.toAbsolutePath().toString();
    }

    private void setString(Sheet sheet, int rowIndex, int columnIndex, String value) {
        Cell cell = cell(sheet, rowIndex, columnIndex);
        cell.setCellValue(value == null ? "" : value);
    }

    private void setNumber(Sheet sheet, int rowIndex, int columnIndex, double value) {
        cell(sheet, rowIndex, columnIndex).setCellValue(value);
    }

    private void setFormula(Sheet sheet, int rowIndex, int columnIndex, String formula) {
        Cell cell = cell(sheet, rowIndex, columnIndex);
        cell.setCellFormula(formula);
    }

    private Cell cell(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) row = sheet.createRow(rowIndex);
        Cell cell = row.getCell(columnIndex - 1);
        if (cell == null) cell = row.createCell(columnIndex - 1, CellType.BLANK);
        return cell;
    }

    private String paymentStatus(Payment payment) {
        if (n(payment.getBalanceAmount()) <= 0 && n(payment.getTotalAmount()) > 0) return "Paid";
        if (n(payment.getAmountPaid()) > 0) return "Partial Paid";
        return "Pending";
    }

    private String joinClient(String organization, String address) {
        if (organization == null || organization.isBlank()) return nz(address);
        if (address == null || address.isBlank()) return organization;
        return organization + "\n" + address;
    }

    private String formatPaymentDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return java.time.LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception ignored) {
            return value;
        }
    }

    private double n(Double value) { return value == null ? 0d : value; }
    private String nz(String value) { return value == null ? "" : value; }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private String safeName(String value) {
        if (value == null || value.isBlank()) return "invoice";
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // Compact Indian-number wording for the amount field in the supplied template.
    private String amountInWords(double amount) {
        long value = Math.round(amount);
        if (value == 0) return "Zero";
        String[] small = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
        return words(value, small, tens).trim();
    }

    private String words(long value, String[] small, String[] tens) {
        if (value < 20) return small[(int) value];
        if (value < 100) return tens[(int) (value / 10)] + (value % 10 == 0 ? "" : " " + small[(int) (value % 10)]);
        if (value < 1000) return small[(int) (value / 100)] + " Hundred" + (value % 100 == 0 ? "" : " and " + words(value % 100, small, tens));
        if (value < 100000) return words(value / 1000, small, tens) + " Thousand" + (value % 1000 == 0 ? "" : " " + words(value % 1000, small, tens));
        if (value < 10000000) return words(value / 100000, small, tens) + " Lakh" + (value % 100000 == 0 ? "" : " " + words(value % 100000, small, tens));
        return words(value / 10000000, small, tens) + " Crore" + (value % 10000000 == 0 ? "" : " " + words(value % 10000000, small, tens));
    }
}