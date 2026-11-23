package com.indocrm.indocrm.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.indocrm.indocrm.model.Quotation;
import com.indocrm.indocrm.model.QuotationTemplate;
import com.indocrm.indocrm.model.QuotationTemplate.DesignBlock;
import com.indocrm.indocrm.model.QuotationTemplate.DesignPage;
import com.indocrm.indocrm.model.QuotationTemplate.TemplateDesign;
import com.indocrm.indocrm.model.QuotationTemplate.TemplateField;

/**
 * Built-in quotation layouts that reproduce the three approved IndoCampas PDFs:
 *   1. ERP quotation            (Shanthi Rani Matric school sample)
 *   2. Digital Marketing quote  (Selva Infotech + Kids Kingdom samples)
 *
 * Nothing here reads a template file from disk, so the
 * "The system cannot find the path specified" error can no longer happen.
 * The layouts are drawn with the existing PDFBox "BUILDER" renderer.
 */
@Component
public class QuotationLayouts {

    public static final String ERP_ID = "builtin-erp";
    public static final String DM_ID  = "builtin-digital-marketing";
    public static final String CDM_ID = "builtin-commercial-digital-marketing";
    private static final String MODE_ERP = "BUILTIN_ERP";
    private static final String MODE_DM  = "BUILTIN_DM";

    private static final BigDecimal GST_RATE = new BigDecimal("0.18");
    private static final String BORDER = "#111827";
    private static final double A4_W = 595.28;
    private static final double A4_H = 841.89;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    // ───────────────────────── public API ─────────────────────────

    public static boolean isBuiltIn(String id) { return id != null && id.startsWith("builtin-"); }

    public static boolean isBuiltInDesign(TemplateDesign d) {
        return d != null && d.getMode() != null && d.getMode().toUpperCase().startsWith("BUILTIN_");
    }

    public List<QuotationTemplate> builtInTemplates() {
        List<QuotationTemplate> list = new ArrayList<>();
        list.add(erpTemplate());
        list.add(digitalMarketingTemplate());
        list.add(commercialDigitalMarketingTemplate());
        return list;
    }

    public QuotationTemplate byId(String id) {
        if (ERP_ID.equals(id)) return erpTemplate();
        if (DM_ID.equals(id)) return digitalMarketingTemplate();
        if (CDM_ID.equals(id)) return commercialDigitalMarketingTemplate();
        throw new RuntimeException("Template not found: " + id);
    }

    public QuotationTemplate forServiceType(String serviceType) {
        String s = serviceType == null ? "" : serviceType.toLowerCase();
        return s.contains("marketing") ? digitalMarketingTemplate() : erpTemplate();
    }

    /** Turns a built-in template + the quotation's data into a concrete BUILDER template that
     *  QuotationPdfService.generateBuilderTemplate() can draw. */
    public QuotationTemplate materialize(Quotation q, QuotationTemplate src, List<DesignBlock> letterhead) {
        boolean dm = MODE_DM.equalsIgnoreCase(src.getDesign().getMode());

        List<DesignBlock> blocks = new ArrayList<>(letterhead);
        if (dm) dmBlocks(blocks, q); else erpBlocks(blocks, q);

        TemplateDesign d = brandDesign();
        d.setMode("BUILDER");
        DesignPage page = new DesignPage();
        page.setId("page_1");
        page.setName("Page 1");
        page.setBackgroundColor("#FFFFFF");
        page.setHeaderEnabled(false);
        page.setFooterEnabled(false);
        page.setBlocks(blocks);
        List<DesignPage> pages = new ArrayList<>();
        pages.add(page);
        d.setPages(pages);
        d.setBlocks(blocks);

        QuotationTemplate out = new QuotationTemplate();
        out.setId(src.getId());
        out.setName(src.getName());
        out.setPdfSource(false);
        out.setPageWidthPt(A4_W);
        out.setPageHeightPt(A4_H);
        out.setFields(src.getFields());
        out.setDesign(d);
        return out;
    }

    // ───────────────────── template definitions ─────────────────────

    private QuotationTemplate erpTemplate() {
        QuotationTemplate t = base(ERP_ID, "IndoCampas ERP Quotation", "ERP", MODE_ERP);
        List<TemplateField> f = new ArrayList<>();
        f.add(field("studentStrength", "Student Strength *", ""));
        f.add(field("attn", "Addressed To", "The Principal"));
        f.add(field("ratePro", "ERP PRO - rate per student (default 60)", "60"));
        f.add(field("ratePlus", "ERP PRO PLUS - rate per student (default 120)", "120"));
        f.add(field("rateMax", "ERP PRO MAX - rate per student (default 200)", "200"));
        f.add(field("freeDashboardText", "Admission Dashboard line", "Online Dashboard absolutely FREE"));
        f.add(field("freeMarketingText", "Digital Marketing line", "Digital Marketing: 2 Videos absolutely FREE"));
        f.add(field("admissionCharge", "Admission charge per admitted student (default 2000)", "2000"));
        f.add(field("specialOffer", "Special Offer text (type - to hide)",
                "ERP PRO free with the combined Admission Dashboard and Digital Marketing package, subject to confirmation."));
        t.setFields(f);
        return t;
    }

    private QuotationTemplate digitalMarketingTemplate() {
        QuotationTemplate t = base(DM_ID, "IndoCampas Digital Marketing Quotation", "Digital Marketing", MODE_DM);
        List<TemplateField> f = new ArrayList<>();
        f.add(field("attn", "Attn (e.g. The Principal / Mr. Selva raj)", "The Principal"));
        f.add(field("videosQty", "Promotional Videos (qty)", "4"));
        f.add(field("postersQty", "Social Media Posters (qty)", "4"));
        f.add(field("duration", "Duration", "1 Month"));
        f.add(field("packageAmount", "Package Amount (Rs, before GST) *", ""));
        f.add(field("discount", "Package Discount (Rs, optional)", ""));
        f.add(field("paymentTerms", "Payment", "100% Advance."));
        f.add(field("gstNote", "Extra note (e.g. GST Applicable.)", ""));
        t.setFields(f);
        return t;
    }

    /** Same layout as Digital Marketing, offered under the "Commercial Digital Marketing" service type. */
    private QuotationTemplate commercialDigitalMarketingTemplate() {
        QuotationTemplate t = digitalMarketingTemplate();
        t.setId(CDM_ID);
        t.setName("IndoCampas Commercial Digital Marketing Quotation");
        t.setServiceType("Commercial Digital Marketing");
        return t;
    }

    private QuotationTemplate base(String id, String name, String serviceType, String mode) {
        QuotationTemplate t = new QuotationTemplate();
        t.setId(id);
        t.setName(name);
        t.setServiceType(serviceType);
        t.setCreatedBy("system");
        t.setPdfSource(false);
        t.setPageWidthPt(A4_W);
        t.setPageHeightPt(A4_H);
        TemplateDesign d = brandDesign();
        d.setMode(mode);
        t.setDesign(d);
        return t;
    }

    private TemplateDesign brandDesign() {
        TemplateDesign d = new TemplateDesign();
        d.setCompanyName("INDO CAMPAS TECH");
        d.setCompanySubtitle("Private Limited");
        d.setCompanyPhone("8925811993 / 8925811994");
        d.setCompanyWebsite("www.indocampas.com");
        d.setCompanyAddress("TIDEL NEO - Thiruchitrambalam\nKoot Road, Tamil Nadu 605111");
        d.setPrimaryColor("#7B1113");
        d.setBackgroundColor("#FFFFFF");
        d.setFontFamily("Times");
        d.setHeaderEnabled(false);
        d.setFooterEnabled(false);
        return d;
    }

    private TemplateField field(String id, String label, String def) {
        TemplateField f = new TemplateField();
        f.setId(id);
        f.setLabel(label);
        f.setMapsTo(id);
        f.setDefaultValue(def);
        return f;
    }

    // ───────────────────────── document data ─────────────────────────
    // The New Quotation screen sends the whole editable document as JSON in fieldValues["doc"].
    // Quotations created before that (simple template fields) are converted to the same shape.

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode docFor(Quotation q, boolean dm) {
        String raw = q.getFieldValues() == null ? null : q.getFieldValues().get("doc");
        if (raw != null && !raw.isBlank()) {
            try {
                JsonNode n = MAPPER.readTree(raw);
                if (n != null && n.isObject()) return n;
            } catch (Exception ignored) { /* fall through to legacy */ }
        }
        return legacyDoc(q, dm);
    }

    private ObjectNode legacyDoc(Quotation q, boolean dm) {
        ObjectNode d = MAPPER.createObjectNode();
        d.put("_legacy", true);
        d.put("attn", val(q, "attn", "The Principal"));
        d.put("gstPercent", "18");
        if (!dm) {
            d.put("strength", val(q, "studentStrength", ""));
            ArrayNode plans = d.putArray("plans");
            plans.addObject().put("name", "ERP PRO - Per Student/Year").put("rate", val(q, "ratePro", "60"));
            plans.addObject().put("name", "ERP PRO PLUS - Per Student/Year").put("rate", val(q, "ratePlus", "120"));
            plans.addObject().put("name", "ERP PRO MAX - Per Student/Year").put("rate", val(q, "rateMax", "200"));
            ArrayNode free = d.putArray("freeRows");
            free.addObject().put("label", "Admission Dashboard").put("text", val(q, "freeDashboardText", "Online Dashboard absolutely FREE"));
            free.addObject().put("label", "Digital Marketing").put("text", val(q, "freeMarketingText", "Digital Marketing: 2 Videos absolutely FREE"));
            ArrayNode notes = d.putArray("notes");
            notes.addObject().put("label", "Admission Dashboard:").put("text", "Online admissions, student data management and reports.");
            notes.addObject().put("label", "Digital Marketing:").put("text", "Social media promotions and admission lead generation.");
            notes.addObject().put("label", "Admission Charges:")
                    .put("text", inr(num(val(q, "admissionCharge", "2000"), new BigDecimal("2000"))) + " per student admitted through our platform.");
            String offer = val(q, "specialOffer",
                    "ERP PRO free with the combined Admission Dashboard and Digital Marketing package, subject to confirmation.");
            if (!offer.trim().equals("-")) notes.addObject().put("label", "Special Offer:").put("text", offer);
        } else {
            ArrayNode svc = d.putArray("services");
            svc.addObject().put("name", "Promotional Videos").put("qty", val(q, "videosQty", "-"));
            svc.addObject().put("name", "Social Media Posters").put("qty", val(q, "postersQty", "-"));
            svc.addObject().put("name", "Duration").put("qty", val(q, "duration", "1 Month"));
            d.put("package", val(q, "packageAmount", ""));
            d.put("discount", val(q, "discount", ""));
            d.putArray("extras");
            d.put("payment", val(q, "paymentTerms", "100% Advance."));
            ArrayNode notes = d.putArray("notes");
            String note = val(q, "gstNote", "");
            if (!note.isBlank()) notes.addObject().put("text", note);
        }
        return d;
    }

    private String s(JsonNode n, String key, String def) {
        JsonNode v = n == null ? null : n.get(key);
        if (v == null || v.isNull()) return def;
        String t = v.asText();
        return t == null || t.isBlank() ? def : t.trim();
    }

    private BigDecimal pct(JsonNode d) {
        return num(s(d, "gstPercent", "18"), new BigDecimal("18")).divide(new BigDecimal("100"));
    }

    private String pctLabel(JsonNode d) {
        return num(s(d, "gstPercent", "18"), new BigDecimal("18")).stripTrailingZeros().toPlainString();
    }

    /** Rough number of text lines a paragraph needs (for block height). */
    private int linesFor(String text, int charsPerLine) {
        int lines = 0;
        for (String part : nvl(text).split("\n", -1)) lines += Math.max(1, (int) Math.ceil(part.length() / (double) charsPerLine));
        return lines;
    }

    // ───────────────────────── layout (all measurements in PDF points) ─────────────────────────
    // A4 = 595 x 842 pt. Content area: x 56..539. Letterpad ribbons: top-right (y < 120) and
    // bottom-left (y > 645, x < 70); the footer sits at y 705..800, x 83..400.

    private static final double ML = 56, CW = 483;          // left margin, content width
    private static final double BODY = 11, TBL = 10.5;      // font sizes
    private static final double LINE = 18;                  // height of one info line
    private static final double CONTENT_MAX_Y = 650;        // content must end above the bottom ribbon

    private double fx(double pt) { return pt / A4_W; }
    private double fy(double pt) { return pt / A4_H; }

    private DesignBlock tp(List<DesignBlock> b, String id, double x, double y, double w, double h,
                           String text, double size, boolean bold, String align) {
        return text(b, id, fx(x), fy(y), fx(w), fy(h), text, size, bold, align);
    }

    private void cp(List<DesignBlock> b, String id, double x, double y, double w, double h,
                    String text, boolean bold, String align) {
        cell(b, id, fx(x), fy(y), fx(w), fy(h), text, bold, align);
    }

    /** bold label + normal value on one line */
    private void lv(List<DesignBlock> b, String id, double x, double y, double labelW, String label,
                    double valueW, String value) {
        tp(b, id + "L", x, y, labelW, LINE, label, BODY, true, "left");
        tp(b, id + "V", x + labelW, y, valueW, LINE, value, BODY, false, "left");
    }

    /** height of a table row whose first-column text may wrap */
    private double rowH(String text, double colW, double min) {
        int lines = Math.max(1, (int) Math.ceil(nvl(text).length() * 5.6 / Math.max(20, colW - 10)));
        return Math.max(min, lines * 13 + 12);
    }

    private void title(List<DesignBlock> b, String t) {
        tp(b, "title", ML, 138, CW, 24, t, 14, true, "center");
    }

    private void signature(List<DesignBlock> b, double y, JsonNode d) {
        double x = 330, w = 209;
        tp(b, "sig1", x, y, w, LINE, s(d, "signCompany", "For INDOCAMPAS TECH PRIVATE LIMITED"), 9.5, true, "left");
        tp(b, "sig2", x, y + 26, w, LINE, s(d, "signName", "R. Aravinda Raj"), BODY, false, "left");
        tp(b, "sig3", x, y + 44, w, LINE, s(d, "signTitle", "Chief Executive Officer"), BODY, false, "left");
    }

    private void checkFits(double y) {
        if (y > CONTENT_MAX_Y) {
            throw new IllegalArgumentException(
                    "Too many rows to fit on one page - please remove a few rows or shorten the notes.");
        }
    }

    // ───────────────────────── ERP layout ─────────────────────────

    private void erpBlocks(List<DesignBlock> b, Quotation q) {
        JsonNode d = docFor(q, false);
        BigDecimal strength = num(s(d, "strength", ""), BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
        if (strength.signum() <= 0) {
            throw new IllegalArgumentException("Student Strength is required for an ERP quotation.");
        }
        BigDecimal gstRate = pct(d);
        String subject = nvl(q.getSubject()).trim();
        if (subject.isEmpty()) subject = "ERP, Student Strength ( " + strength.toPlainString() + " )";
        else if (d.path("_legacy").asBoolean(false) && !subject.toLowerCase().contains("strength"))
            subject += ", Student Strength ( " + strength.toPlainString() + " )";

        title(b, s(d, "title", "QUOTATION"));

        double y = 174;
        lv(b, "qno", ML, y, 92, "Quotation No.:", 230, nvl(q.getQuotationNo()));
        lv(b, "date", 399, y, 38, "Date:", 102, s(d, "date", dateText(q)));
        y += LINE + 8;
        lv(b, "to", ML, y, 28, "To:", 440, s(d, "attn", "The Principal") + ",");
        y += LINE;
        tp(b, "org", ML, y, CW, LINE, nvl(q.getOrganization()).toUpperCase(), 11.5, true, "left");
        y += LINE;
        lv(b, "subj", ML, y, 52, "Subject:", 430, subject);
        y += LINE + 12;

        // ── pricing table ──
        double[] cw = {190, 70, 78, 70, 75};
        String[] hdr = {"Service", "Per Student", "Amount", "GST", "Total"};
        double x = ML;
        for (int c = 0; c < 5; c++) { cp(b, "h" + c, x, y, cw[c], 28, hdr[c], true, "center"); x += cw[c]; }
        y += 28;

        JsonNode plans = d.path("plans");
        if (!plans.isArray() || plans.size() == 0) throw new IllegalArgumentException("Add at least one ERP plan row.");
        int r = 0;
        for (JsonNode p : plans) {
            BigDecimal rate = num(s(p, "rate", ""), BigDecimal.ZERO);
            BigDecimal amount = rate.multiply(strength);
            BigDecimal gst = amount.multiply(gstRate);
            String name = s(p, "name", "-");
            double h = rowH(name, cw[0], 26);
            String[] vals = {name, inr(rate), inr(amount), inr(gst), inr(amount.add(gst))};
            x = ML;
            for (int c = 0; c < 5; c++) {
                cp(b, "r" + r + "c" + c, x, y, cw[c], h, vals[c], c <= 1, c == 0 ? "left" : "center");
                x += cw[c];
            }
            y += h; r++;
        }
        int f = 0;
        for (JsonNode fr : d.path("freeRows")) {
            String txt = s(fr, "text", "-");
            double h = rowH(txt, CW - cw[0], 26);
            cp(b, "f" + f + "a", ML, y, cw[0], h, s(fr, "label", "-"), true, "left");
            cp(b, "f" + f + "b", ML + cw[0], y, CW - cw[0], h, txt, true, "left");
            y += h; f++;
        }
        y += 16;

        // ── notes ──
        double labelW = 118;
        int n = 0;
        for (JsonNode note : d.path("notes")) {
            String label = nvl(note.path("label").asText()).trim();
            String txt = nvl(note.path("text").asText()).trim();
            if (label.isEmpty() && txt.isEmpty()) continue;
            double valueX = label.isEmpty() ? ML : ML + labelW;
            double valueW = label.isEmpty() ? CW : CW - labelW;
            int lines = linesFor(txt, (int) (valueW / 5.3));
            double h = lines * 14 + 4;
            if (!label.isEmpty()) tp(b, "n" + n + "L", ML, y, labelW, 16, label, BODY, true, "left").setVerticalAlign("top");
            tp(b, "n" + n + "V", valueX, y, valueW, h, txt, BODY, false, "left").setVerticalAlign("top");
            y += h + 5; n++;
        }
        y += 14;
        checkFits(y);
        signature(b, y, d);
    }

    // ───────────────────── Digital Marketing layout ─────────────────────

    private void dmBlocks(List<DesignBlock> b, Quotation q) {
        JsonNode d = docFor(q, true);
        BigDecimal pkg = num(s(d, "package", ""), BigDecimal.ZERO);
        if (pkg.signum() <= 0) throw new IllegalArgumentException("Package Amount is required for a Digital Marketing quotation.");
        BigDecimal discount = num(s(d, "discount", ""), BigDecimal.ZERO);
        BigDecimal gst = pkg.multiply(pct(d));
        BigDecimal total = pkg.add(gst).subtract(discount);
        String subject = nvl(q.getSubject()).trim();
        if (subject.isEmpty()) subject = "Digital Marketing Services";

        title(b, s(d, "title", "DIGITAL MARKETING QUOTATION"));

        double y = 174;
        lv(b, "qno", ML, y, 92, "Quotation No.:", 230, nvl(q.getQuotationNo()));
        lv(b, "date", 399, y, 38, "Date:", 102, s(d, "date", dateText(q)));
        y += LINE + 8;
        lv(b, "to", ML, y, 28, "To:", 440, nvl(q.getOrganization()));
        y += LINE;
        lv(b, "attn", ML, y, 38, "Attn:", 430, s(d, "attn", "The Principal"));
        y += LINE;
        if (!nvl(q.getAddress()).isBlank()) {
            tp(b, "addr", ML, y, CW, LINE, nvl(q.getAddress()).replace("\n", ", "), BODY, false, "left");
            y += LINE;
        }
        lv(b, "subj", ML, y, 52, "Subject:", 430, subject);
        y += LINE + 14;

        double c0 = 280, c1 = 130, hdrH = 28, rowH = 26;
        cp(b, "sh0", ML, y, c0, hdrH, "Service", true, "center");
        cp(b, "sh1", ML + c0, y, c1, hdrH, "Qty", true, "center");
        y += hdrH;
        int i = 0;
        for (JsonNode sv : d.path("services")) {
            cp(b, "s" + i + "a", ML, y, c0, rowH, s(sv, "name", "-"), false, "left");
            cp(b, "s" + i + "b", ML + c0, y, c1, rowH, s(sv, "qty", "-"), false, "center");
            y += rowH; i++;
        }

        y += 14;
        tp(b, "qamt", ML, y, 250, LINE, "Quotation Amount", 11.5, true, "left");
        y += LINE + 4;
        cp(b, "ah0", ML, y, c0, hdrH, "Particulars", true, "center");
        cp(b, "ah1", ML + c0, y, c1, hdrH, "Amount", true, "center");
        y += hdrH;

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Package Amount", inr(pkg), "n"});
        rows.add(new String[]{"GST @ " + pctLabel(d) + "%", inr(gst), "n"});
        if (discount.signum() > 0) rows.add(new String[]{"Package Discount", "-" + inr(discount), "n"});
        for (JsonNode ex : d.path("extras")) {
            String label = s(ex, "label", "");
            BigDecimal amt = num(s(ex, "amount", ""), BigDecimal.ZERO);
            if (label.isEmpty() && amt.signum() == 0) continue;
            total = total.add(amt);
            rows.add(new String[]{label.isEmpty() ? "-" : label, inr(amt), "n"});
        }
        rows.add(new String[]{"Final Amount", inr(total), "b"});
        for (int k = 0; k < rows.size(); k++) {
            boolean bold = "b".equals(rows.get(k)[2]);
            cp(b, "a" + k + "a", ML, y, c0, rowH, rows.get(k)[0], bold, "left");
            cp(b, "a" + k + "b", ML + c0, y, c1, rowH, rows.get(k)[1], bold, "center");
            y += rowH;
        }

        y += 14;
        lv(b, "pay", ML, y, 52, "Payment:", 430, s(d, "payment", "100% Advance."));
        y += LINE + 2;
        int n = 0;
        for (JsonNode note : d.path("notes")) {
            String t = nvl(note.path("text").asText()).trim();
            if (t.isEmpty()) continue;
            tp(b, "note" + n, ML, y, CW, LINE, t, BODY, true, "left");
            y += LINE; n++;
        }
        y += 18;
        checkFits(y);
        signature(b, y, d);
    }

    // ───────────────────────── block helpers ─────────────────────────

    private void labelValue(List<DesignBlock> b, String id, double x, double y,
                            double labelW, String label, double valueW, String value, double size) {
        text(b, id + "L", x, y, labelW, 0.028, label, size, true, "left");
        text(b, id + "V", x + labelW, y, valueW, 0.028, value, size, false, "left");
    }

    private void paragraph(List<DesignBlock> b, String id, double lx, double lw, double vx, double vw,
                           double y, double h, String label, String value) {
        DesignBlock l = text(b, id + "L", lx, y, lw, h, label, 10.5, true, "left");
        DesignBlock v = text(b, id + "V", vx, y, vw, h, value, 10.5, false, "left");
        l.setVerticalAlign("top");
        v.setVerticalAlign("top");
    }

    private void cell(List<DesignBlock> b, String id, double x, double y, double w, double h,
                      String text, boolean bold, String align) {
        DesignBlock c = text(b, id, x, y, w, h, (text == null || text.isBlank()) ? " " : text, 10, bold, align);
        c.setBackgroundColor("#FFFFFF");
        c.setBorderWidth(0.8);
        c.setBorderColor(BORDER);
        c.setVerticalAlign("middle");
        c.setPadding(5);
    }

    private DesignBlock text(List<DesignBlock> b, String id, double x, double y, double w, double h,
                             String text, double size, boolean bold, String align) {
        DesignBlock d = new DesignBlock();
        d.setId(id);
        d.setType("text");
        d.setX(x); d.setY(y); d.setWidth(w); d.setHeight(h);
        d.setText(text);
        d.setMapsTo("");
        d.setFontSize(size);
        d.setFontFamily("Times");
        d.setBold(bold);
        d.setColor("#111827");
        d.setBackgroundColor("transparent");
        d.setAlign(align);
        d.setVerticalAlign("middle");
        d.setPadding(2);
        b.add(d);
        return d;
    }

    // ───────────────────────── value helpers ─────────────────────────

    private String val(Quotation q, String key, String def) {
        Map<String, String> m = q.getFieldValues();
        String v = m == null ? null : m.get(key);
        return v == null || v.isBlank() ? def : v.trim();
    }

    private BigDecimal num(String s, BigDecimal def) {
        if (s == null) return def;
        String cleaned = s.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) return def;
        try { return new BigDecimal(cleaned); } catch (NumberFormatException e) { return def; }
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private String dateText(Quotation q) {
        Instant at = q.getCreatedAt() != null ? q.getCreatedAt() : Instant.now();
        return DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(IST).format(at);
    }

    /** Rupee amount with Indian digit grouping (1,08,000). QuotationPdfService.safePdfText turns the
     *  rupee sign into "Rs." because the built-in PDF fonts have no rupee glyph. */
    static String inr(BigDecimal v) {
        v = v.setScale(2, RoundingMode.HALF_UP);
        boolean neg = v.signum() < 0;
        String[] parts = v.abs().toPlainString().split("\\.");
        String ip = parts[0];
        String grouped;
        if (ip.length() <= 3) {
            grouped = ip;
        } else {
            String last3 = ip.substring(ip.length() - 3);
            String rest = ip.substring(0, ip.length() - 3);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rest.length(); i++) {
                if (i > 0 && (rest.length() - i) % 2 == 0) sb.append(',');
                sb.append(rest.charAt(i));
            }
            grouped = sb + "," + last3;
        }
        String out = "\u20B9" + grouped + ("00".equals(parts[1]) ? "" : "." + parts[1]);
        return neg ? "-" + out : out;
    }
}