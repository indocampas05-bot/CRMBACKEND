package com.indocrm.indocrm.service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.model.Quotation;
import com.indocrm.indocrm.model.QuotationTemplate;
import com.indocrm.indocrm.model.QuotationTemplate.DesignBlock;
import com.indocrm.indocrm.model.QuotationTemplate.DesignPage;
import com.indocrm.indocrm.model.QuotationTemplate.TemplateDesign;
import com.indocrm.indocrm.model.QuotationTemplate.TemplateField;

@Service
public class QuotationPdfService {

    @Value("${quotation.storage.dir:./storage/quotations}")
    private String storageDirProp;

    private final QuotationLayouts layouts;
    private final QuotationBrandingService branding;

    public QuotationPdfService(QuotationLayouts layouts, QuotationBrandingService branding) {
        this.layouts = layouts;
        this.branding = branding;
    }

    private Path outputDir() throws Exception {
        Path dir = Paths.get(storageDirProp, "generated");
        Files.createDirectories(dir);
        return dir;
    }

    public String generate(Quotation quotation, QuotationTemplate template) throws Exception {
        Path outPath = outputDir().resolve(safeName(quotation.getId()) + ".pdf");
        TemplateDesign design = template.getDesign();

        // Built-in IndoCampas layouts (ERP / Digital Marketing) - drawn fully in code,
        // no template file on disk is needed.
        if (QuotationLayouts.isBuiltInDesign(design)) {
            QuotationTemplate concrete = layouts.materialize(quotation, template, branding.buildLetterhead());
            generateBuilderTemplate(quotation, concrete, outPath);
            return outPath.toAbsolutePath().toString();
        }

        if (design == null || "BACKGROUND".equalsIgnoreCase(design.getMode())) {
            generateBackgroundTemplate(quotation, template, outPath);
        } else {
            generateBuilderTemplate(quotation, template, outPath);
        }
        return outPath.toAbsolutePath().toString();
    }

    private void generateBackgroundTemplate(Quotation quotation, QuotationTemplate template, Path outPath) throws Exception {
        String renderSource = template.getStoredRenderPdfPath() != null && !template.getStoredRenderPdfPath().isBlank()
                ? template.getStoredRenderPdfPath()
                : template.getStoredOriginalPath();
        if (renderSource != null && !renderSource.isBlank() && !new File(renderSource).isFile()) {
            throw new IllegalStateException("The template file is missing on the server (" + renderSource
                    + "). Please choose one of the built-in IndoCampas templates.");
        }
        if (template.isPdfSource() && renderSource != null) {
            try (PDDocument doc = PDDocument.load(new File(renderSource))) {
                int pages = doc.getNumberOfPages();
                for (int pageIndex = 0; pageIndex < pages; pageIndex++) {
                    PDPage page = doc.getPage(pageIndex);
                    // Existing fields are defined against page 1 in the current editor.
                    // Render them only on the first page to preserve old template behavior.
                    if (pageIndex == 0) drawFields(doc, page, template, quotation);
                }
                doc.save(outPath.toFile());
            }
            return;
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle((float) template.getPageWidthPt(), (float) template.getPageHeightPt()));
            doc.addPage(page);
            if (template.getStoredOriginalPath() != null) {
                PDImageXObject image = PDImageXObject.createFromFile(template.getStoredOriginalPath(), doc);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(image, 0, 0, (float) template.getPageWidthPt(), (float) template.getPageHeightPt());
                }
            }
            drawFields(doc, page, template, quotation);
            doc.save(outPath.toFile());
        }
    }

    private void drawFields(PDDocument doc, PDPage page, QuotationTemplate template, Quotation quotation) throws Exception {
        Map<String, String> values = quotation.getFieldValues() == null ? new HashMap<>() : quotation.getFieldValues();
        for (TemplateField field : template.getFields()) {
            String text = values.get(field.getId());
            if (text == null || text.isBlank()) continue;
            renderTextBox(doc, page, field.getX(), field.getY(), field.getWidth(), field.getHeight(),
                    text, field.getFontSize(), field.getFontFamily(), field.isBold() || "bold".equalsIgnoreCase(field.getFontWeight()),
                    field.isItalic(), field.getColor(), field.getAlign(), field.getVerticalAlign(), field.getPadding());
        }
    }

    private void generateBuilderTemplate(Quotation quotation, QuotationTemplate template, Path outPath) throws Exception {
        TemplateDesign d = template.getDesign();
        double width = template.getPageWidthPt() > 0 ? template.getPageWidthPt() : 595.28;
        double height = template.getPageHeightPt() > 0 ? template.getPageHeightPt() : 841.89;

        if ("LANDSCAPE".equalsIgnoreCase(d.getOrientation())) {
            double tmp = width; width = height; height = tmp;
        }

        List<DesignPage> pages = d.getPages();
        if (pages == null || pages.isEmpty()) {
            DesignPage legacy = new DesignPage();
            legacy.setId("page_1");
            legacy.setName("Page 1");
            legacy.setBackgroundColor(d.getBackgroundColor());
            legacy.setHeaderEnabled(d.isHeaderEnabled());
            legacy.setHeaderHeight(d.getHeaderHeight());
            legacy.setHeaderLayout(d.getHeaderLayout());
            legacy.setFooterEnabled(d.isFooterEnabled());
            legacy.setFooterHeight(d.getFooterHeight());
            legacy.setFooterLayout(d.getFooterLayout());
            legacy.setBlocks(safeBlocks(d.getBlocks()));
            pages = List.of(legacy);
        }

        try (PDDocument doc = new PDDocument()) {
            for (DesignPage pageDesign : pages) {
                PDPage page = new PDPage(new PDRectangle((float) width, (float) height));
                doc.addPage(page);

                String pageBackground = pageDesign.getBackgroundColor();
                if (pageBackground == null || pageBackground.isBlank()) pageBackground = d.getBackgroundColor();
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.setNonStrokingColor(parseColor(pageBackground, Color.WHITE));
                    cs.addRect(0, 0, (float) width, (float) height);
                    cs.fill();
                }

                if (pageDesign.isHeaderEnabled()) renderHeader(doc, page, d, pageDesign, width, height);
                if (pageDesign.isFooterEnabled()) renderFooter(doc, page, d, pageDesign, width, height);

                for (DesignBlock block : safeBlocks(pageDesign.getBlocks())) {
                    renderBlock(doc, page, block, quotation, d, width, height);
                }
            }
            doc.save(outPath.toFile());
        }
    }

    private void renderHeader(PDDocument doc, PDPage page, TemplateDesign d, DesignPage p,
                              double pageW, double pageH) throws Exception {
        double h = clamp(p.getHeaderHeight(), 0.05, 0.35) * pageH;
        String layout = p.getHeaderLayout() == null ? "logo-left" : p.getHeaderLayout();
        boolean hasLogo = d.getLogoDataUrl() != null && !d.getLogoDataUrl().isBlank();

        try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            cs.setNonStrokingColor(parseColor(d.getSecondaryColor(), Color.DARK_GRAY));
            cs.addRect(
        0f,
        (float) (pageH - h),
        (float) pageW,
        (float) h
);
            cs.fill();

            if (hasLogo) {
                try {
                    PDImageXObject logo = imageFromDataUrl(doc, d.getLogoDataUrl());
                    if (logo != null) {
                        double logoSize = Math.min(h * 0.72, pageW * 0.16);
                        double logoX;
                        if ("logo-center".equalsIgnoreCase(layout)) logoX = (pageW - logoSize) / 2;
                        else if ("logo-right".equalsIgnoreCase(layout)) logoX = pageW - logoSize - 28;
                        else logoX = 28;
                        double logoY = pageH - h + (h - logoSize) / 2;
                        cs.drawImage(logo, (float) logoX, (float) logoY, (float) logoSize, (float) logoSize);
                    }
                } catch (Exception ignored) {}
            }
        }

        double textWidth = pageW * 0.70;
        double textX;
        String textAlign;
        if ("logo-center".equalsIgnoreCase(layout)) {
            textX = pageW * 0.15; textAlign = "center";
        } else if ("logo-right".equalsIgnoreCase(layout)) {
            textX = 32; textAlign = "left";
        } else {
            textX = hasLogo ? pageW * 0.19 : 32; textAlign = "left";
        }

        renderTextBox(doc, page, textX / pageW, (pageH - h + h * 0.16) / pageH,
                textWidth / pageW, 0.08, d.getCompanyName(), 18, d.getFontFamily(), true, false,
                "#FFFFFF", textAlign, "middle", 0, "transparent", 0, "#FFFFFF", d.getFontDataUrl());

        if (d.getCompanySubtitle() != null && !d.getCompanySubtitle().isBlank()) {
            renderTextBox(doc, page, textX / pageW, (pageH - h + h * 0.52) / pageH,
                    textWidth / pageW, 0.05, d.getCompanySubtitle(), 10, d.getFontFamily(), false, false,
                    "#F3F4F6", textAlign, "middle", 0, "transparent", 0, "#FFFFFF", d.getFontDataUrl());
        }
    }

    private void renderFooter(PDDocument doc, PDPage page, TemplateDesign d, DesignPage p,
                              double pageW, double pageH) throws Exception {
        double h = clamp(p.getFooterHeight(), 0.05, 0.25) * pageH;
        String layout = p.getFooterLayout() == null ? "contact-center" : p.getFooterLayout();
        String contact = joinNonBlank(" • ", d.getCompanyPhone(), d.getCompanyEmail(), d.getCompanyWebsite());
        String address = d.getCompanyAddress();
        String gst = d.getCompanyGstNumber();
        String align = "center";
        if ("contact-left".equalsIgnoreCase(layout)) align = "left";
        if ("contact-right".equalsIgnoreCase(layout)) align = "right";

        try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            cs.setNonStrokingColor(parseColor(d.getPrimaryColor(), Color.DARK_GRAY));
            cs.addRect(0, 0, (float) pageW, (float) h);
            cs.fill();
        }

        if (!contact.isBlank()) {
            renderTextBox(doc, page, 0.06, 0.025, 0.88, 0.045, contact, 9, d.getFontFamily(), false, false,
                    "#FFFFFF", align, "middle", 0, "transparent", 0, "#FFFFFF", d.getFontDataUrl());
        }
        if (!address.isBlank()) {
            renderTextBox(doc, page, 0.06, 0.07, 0.88, 0.045, address, 8.5, d.getFontFamily(), false, false,
                    "#FFFFFF", align, "middle", 0, "transparent", 0, "#FFFFFF", d.getFontDataUrl());
        }
        if (!gst.isBlank()) {
            renderTextBox(doc, page, 0.06, 0.105, 0.88, 0.035, "GST: " + gst, 7.5, d.getFontFamily(), false, false,
                    "#F3F4F6", align, "middle", 0, "transparent", 0, "#FFFFFF", d.getFontDataUrl());
        }
    }

    private void renderBlock(PDDocument doc, PDPage page, DesignBlock b, Quotation q, TemplateDesign d,
                             double pageW, double pageH) throws Exception {
        double x = clamp(b.getX(), 0, 1) * pageW;
        double top = clamp(b.getY(), 0, 1) * pageH;
        double w = clamp(b.getWidth(), 0, 1) * pageW;
        double h = clamp(b.getHeight(), 0, 1) * pageH;
        String text = resolve(b.getText(), q, d);
        String type = b.getType() == null ? "text" : b.getType();

        if ("image".equalsIgnoreCase(type)) {
            if (b.getImageDataUrl() != null && !b.getImageDataUrl().isBlank()) {
                try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    PDImageXObject image = imageFromDataUrl(doc, b.getImageDataUrl());
                    if (image != null) {
                        double pdfY = pageH - top - h;
                        cs.drawImage(image, (float) x, (float) pdfY, (float) w, (float) h);
                    }
                }
            }
            return;
        }

        if ("divider".equalsIgnoreCase(type)) {
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                cs.setStrokingColor(parseColor(b.getColor(), parseColor(d.getPrimaryColor(), Color.DARK_GRAY)));
                cs.setLineWidth((float) Math.max(0.5, b.getBorderWidth() <= 0 ? 1 : b.getBorderWidth()));
                double pdfY = pageH - top - h / 2;
                cs.moveTo((float) x, (float) pdfY);
                cs.lineTo((float) (x + w), (float) pdfY);
                cs.stroke();
            }
            return;
        }

        if ("table".equalsIgnoreCase(type)) {
            renderTable(doc, page, b, q, d, pageW, pageH);
            return;
        }

        String effectiveText = text;
        if ((effectiveText == null || effectiveText.isBlank()) && b.getMapsTo() != null && !b.getMapsTo().isBlank()) effectiveText = "{{" + b.getMapsTo() + "}}";
        if ("signature".equalsIgnoreCase(type) && (effectiveText == null || effectiveText.isBlank())) {
            effectiveText = "Warm regards,\n" + d.getCompanyName();
        }
        if ("spacer".equalsIgnoreCase(type)) return;

        renderTextBox(doc, page, b.getX(), b.getY(), b.getWidth(), b.getHeight(), effectiveText,
                b.getFontSize(), b.getFontFamily() == null || b.getFontFamily().isBlank() ? d.getFontFamily() : b.getFontFamily(),
                b.isBold() || "bold".equalsIgnoreCase(b.getFontWeight()), b.isItalic(),
                b.getColor(), b.getAlign(), b.getVerticalAlign(), b.getPadding(),
                b.getBackgroundColor(), b.getBorderWidth(), b.getBorderColor(),
                b.getFontDataUrl() == null || b.getFontDataUrl().isBlank() ? d.getFontDataUrl() : b.getFontDataUrl());
    }

    private void renderTable(PDDocument doc, PDPage page, DesignBlock b, Quotation q, TemplateDesign d,
                             double pageW, double pageH) throws Exception {
        double x = b.getX() * pageW;
        double top = b.getY() * pageH;
        double w = b.getWidth() * pageW;
        double h = b.getHeight() * pageH;
        List<List<String>> rows = b.getTableRows() == null ? new ArrayList<>() : b.getTableRows();
        if (rows.isEmpty()) rows = List.of(List.of("Description", "Amount"), List.of("{{subject}}", "{{amount}}"));
        int cols = rows.stream().mapToInt(List::size).max().orElse(1);
        double rowH = h / rows.size();
        double colW = w / cols;

        for (int r = 0; r < rows.size(); r++) {
            for (int c = 0; c < cols; c++) {
                String cell = c < rows.get(r).size() ? resolve(rows.get(r).get(c), q, d) : "";
                double cx = x + c * colW;
                double cyTop = top + r * rowH;
                String bg = r == 0 ? d.getPrimaryColor() : "#FFFFFF";
                String color = r == 0 ? "#FFFFFF" : b.getColor();
                renderTextBox(doc, page, cx / pageW, cyTop / pageH, colW / pageW, rowH / pageH,
                        cell, r == 0 ? Math.max(9, b.getFontSize()) : b.getFontSize(), b.getFontFamily(), r == 0 || b.isBold(), false,
                        color, b.getAlign(), "middle", b.getPadding(), bg, Math.max(0.5, b.getBorderWidth() == 0 ? 0.6 : b.getBorderWidth()), b.getBorderColor());
            }
        }
    }

    private void renderTextBox(PDDocument doc, PDPage page, double xFrac, double yFrac, double wFrac, double hFrac,
                               String text, double fontSize, String family, boolean bold, boolean italic,
                               String color, String align, String verticalAlign, double padding) throws Exception {
        renderTextBox(doc, page, xFrac, yFrac, wFrac, hFrac, text, fontSize, family, bold, italic, color,
                align, verticalAlign, padding, "transparent", 0, "#D1D5DB");
    }

    private void renderTextBox(PDDocument doc, PDPage page, double xFrac, double yFrac, double wFrac, double hFrac,
                               String text, double fontSize, String family, boolean bold, boolean italic,
                               String color, String align, String verticalAlign, double padding,
                               String backgroundColor, double borderWidth, String borderColor) throws Exception {
        renderTextBox(doc, page, xFrac, yFrac, wFrac, hFrac, text, fontSize, family, bold, italic,
                color, align, verticalAlign, padding, backgroundColor, borderWidth, borderColor, null);
    }

    private void renderTextBox(PDDocument doc, PDPage page, double xFrac, double yFrac, double wFrac, double hFrac,
                               String text, double fontSize, String family, boolean bold, boolean italic,
                               String color, String align, String verticalAlign, double padding,
                               String backgroundColor, double borderWidth, String borderColor, String fontDataUrl) throws Exception {
        if (text == null || text.isBlank()) return;
        double pageW = page.getMediaBox().getWidth();
        double pageH = page.getMediaBox().getHeight();
        double x = xFrac * pageW;
        double top = yFrac * pageH;
        double w = wFrac * pageW;
        double h = hFrac * pageH;
        double pdfBottom = pageH - top - h;
        double pad = Math.max(0, padding);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            if (backgroundColor != null && !backgroundColor.isBlank() && !"transparent".equalsIgnoreCase(backgroundColor)) {
                cs.setNonStrokingColor(parseColor(backgroundColor, Color.WHITE));
                cs.addRect((float)x, (float)pdfBottom, (float)w, (float)h);
                cs.fill();
            }
            if (borderWidth > 0) {
                cs.setStrokingColor(parseColor(borderColor, Color.LIGHT_GRAY));
                cs.setLineWidth((float)borderWidth);
                cs.addRect((float)x, (float)pdfBottom, (float)w, (float)h);
                cs.stroke();
            }

            PDFont font = selectFont(doc, family, bold, italic, fontDataUrl);
            float size = (float)Math.max(5, Math.min(72, fontSize));
            // Replace rupee sign etc. BEFORE measuring/wrapping - the standard fonts cannot encode them.
            String cleanText = safePdfText(text);
            if (!(font instanceof org.apache.pdfbox.pdmodel.font.PDType0Font)) {
                StringBuilder sb = new StringBuilder();
                for (char ch : cleanText.toCharArray()) sb.append(ch > 255 ? '?' : ch);
                cleanText = sb.toString();
            }
            List<String> lines = wrapText(cleanText, font, size, (float)Math.max(1, w - 2 * pad));
            float lineHeight = size * 1.22f;
            float textHeight = lines.size() * lineHeight;
            float startY;
            if ("middle".equalsIgnoreCase(verticalAlign) || "center".equalsIgnoreCase(verticalAlign)) {
                startY = (float)(pdfBottom + (h + textHeight) / 2 - lineHeight + 2);
            } else if ("bottom".equalsIgnoreCase(verticalAlign)) {
                startY = (float)(pdfBottom + h - pad - size);
            } else {
                startY = (float)(pdfBottom + h - pad - size);
            }

            for (String line : lines) {
                float lineWidth = font.getStringWidth(line) / 1000f * size;
                float tx;
                if ("center".equalsIgnoreCase(align)) tx = (float)(x + (w - lineWidth) / 2);
                else if ("right".equalsIgnoreCase(align)) tx = (float)(x + w - pad - lineWidth);
                else tx = (float)(x + pad);
                tx = Math.max((float)x, Math.min(tx, (float)(x + w - lineWidth)));

                cs.beginText();
                cs.setNonStrokingColor(parseColor(color, Color.DARK_GRAY));
                cs.setFont(font, size);
                cs.newLineAtOffset(tx, startY);
                cs.showText(safePdfText(line));
                cs.endText();
                startY -= lineHeight;
                if (startY < pdfBottom - 1) break;
            }
        }
    }

    private PDFont selectFont(PDDocument doc, String family, boolean bold, boolean italic, String fontDataUrl) throws Exception {
        if (fontDataUrl != null && !fontDataUrl.isBlank()) {
            int comma = fontDataUrl.indexOf(',');
            if (comma > 0) {
                try {
                    byte[] bytes = Base64.getDecoder().decode(fontDataUrl.substring(comma + 1));
                    return org.apache.pdfbox.pdmodel.font.PDType0Font.load(doc, new ByteArrayInputStream(bytes), true);
                } catch (Exception ignored) {
                    // Fall back to a built-in font if an uploaded font cannot be read.
                }
            }
        }
        String f = family == null ? "Helvetica" : family.toLowerCase();
        if (f.contains("times")) {
            if (bold && italic) return PDType1Font.TIMES_BOLD_ITALIC;
            if (bold) return PDType1Font.TIMES_BOLD;
            if (italic) return PDType1Font.TIMES_ITALIC;
            return PDType1Font.TIMES_ROMAN;
        }
        if (f.contains("courier")) {
            if (bold && italic) return PDType1Font.COURIER_BOLD_OBLIQUE;
            if (bold) return PDType1Font.COURIER_BOLD;
            if (italic) return PDType1Font.COURIER_OBLIQUE;
            return PDType1Font.COURIER;
        }
        if (bold && italic) return PDType1Font.HELVETICA_BOLD_OBLIQUE;
        if (bold) return PDType1Font.HELVETICA_BOLD;
        if (italic) return PDType1Font.HELVETICA_OBLIQUE;
        return PDType1Font.HELVETICA;
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws Exception {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.replace("\r", "").split("\\n", -1)) {
            if (paragraph.isEmpty()) { lines.add(""); continue; }
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                if (word.isEmpty()) continue;
                String candidate = current.length() == 0 ? word : current + " " + word;
                float width = font.getStringWidth(candidate) / 1000f * fontSize;
                if (width > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else if (width > maxWidth) {
                    lines.add(word);
                    current.setLength(0);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (current.length() > 0) lines.add(current.toString());
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private String resolve(String text, Quotation q, TemplateDesign d) {
        if (text == null) return "";
        Map<String, String> v = new HashMap<>();
        v.put("clientName", nvl(q.getClientName()));
        v.put("organization", nvl(q.getOrganization()));
        v.put("email", nvl(q.getEmail()));
        v.put("phone", nvl(q.getPhone()));
        v.put("subject", nvl(q.getSubject()));
        v.put("amount", q.getAmount() == null ? "" : nvl(q.getCurrency()) + " " + q.getAmount());
        v.put("validTill", nvl(q.getValidTill()));
        v.put("notes", nvl(q.getNotes()));
        v.put("leadId", nvl(q.getLeadId()));
        v.put("quotationId", nvl(q.getId()));
        v.put("companyName", nvl(d.getCompanyName()));
        v.put("companySubtitle", nvl(d.getCompanySubtitle()));
        v.put("companyAddress", nvl(d.getCompanyAddress()));
        v.put("companyPhone", nvl(d.getCompanyPhone()));
        v.put("companyEmail", nvl(d.getCompanyEmail()));
        v.put("companyWebsite", nvl(d.getCompanyWebsite()));
        v.put("companyGstNumber", nvl(d.getCompanyGstNumber()));
        if (q.getFieldValues() != null) v.putAll(q.getFieldValues());
        String out = text;
        for (Map.Entry<String, String> e : v.entrySet()) out = out.replace("{{" + e.getKey() + "}}", e.getValue());
        return out;
    }

    private PDImageXObject imageFromDataUrl(PDDocument doc, String dataUrl) throws Exception {
        int comma = dataUrl.indexOf(',');
        if (comma < 0) return null;
        byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(comma + 1));
        return PDImageXObject.createFromByteArray(doc, bytes, "template-image");
    }

    private Color parseColor(String hex, Color fallback) {
        try {
            if (hex == null || hex.isBlank() || "transparent".equalsIgnoreCase(hex)) return fallback;
            String s = hex.trim().replace("#", "");
            if (s.length() == 3) s = "" + s.charAt(0)+s.charAt(0)+s.charAt(1)+s.charAt(1)+s.charAt(2)+s.charAt(2);
            return new Color(Integer.parseInt(s, 16));
        } catch (Exception e) { return fallback; }
    }

    private String safePdfText(String text) {
        return text.replace("₹", "Rs.").replace("–", "-").replace("—", "-").replace("•", "-").replace("’", "'").replace("“", "\"").replace("”", "\"");
    }

    private String joinNonBlank(String sep, String... values) {
        StringBuilder b = new StringBuilder();
        for (String v : values) if (v != null && !v.isBlank()) { if (b.length() > 0) b.append(sep); b.append(v); }
        return b.toString();
    }

    private String nvl(String s) { return s == null ? "" : s; }
    private double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private List<DesignBlock> safeBlocks(List<DesignBlock> b) { return b == null ? new ArrayList<>() : b; }
    private String safeName(String id) { return id == null ? java.util.UUID.randomUUID().toString() : id.replaceAll("[^a-zA-Z0-9_-]", "_"); }
}