// FILE: src/main/java/com/indocrm/indocrm/service/AgreementPdfService.java
// REPLACES existing file.
//
// Agreement PDFs now use the SAME letterhead the Super Admin uploads for Quotations
// (Quotations page -> Letterpad / Logo / Footer, stored by QuotationBrandingService):
//   - letterpad : full-page A4 background (uploaded one, else the bundled default)
//   - logo      : uploaded logo, same position/size as on quotations
//   - footer    : phone / website / address with round icons (text editable by Super Admin)
// If nothing is uploaded it falls back to the old built-in IndoCampas letterhead.
//
// The letterhead is stamped on EVERY page with PDFBox after openhtmltopdf renders the body.
// (Drawing it with negative-offset position:fixed HTML boxes was being clipped -> no logo.)
package com.indocrm.indocrm.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.model.Agreement;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Service
public class AgreementPdfService {

    private static final Logger log = LoggerFactory.getLogger(AgreementPdfService.class);

    /** points per millimetre */
    private static final float MM = 72f / 25.4f;

    private static final Color BRAND = new Color(0x7B, 0x11, 0x13);
    private static final Color INK = new Color(0x11, 0x18, 0x27);
    private static final Color LINK = new Color(0x1D, 0x4E, 0xD8);
    private static final Color ADDR = new Color(0xC8, 0x10, 0x2E);

    private final QuotationBrandingService branding;

    @Value("${agreement.storage.dir:./storage/agreements}")
    private String storageDirProp;

    public AgreementPdfService(QuotationBrandingService branding) {
        this.branding = branding;
    }

    private Path outputDir() throws Exception {
        Path dir = Paths.get(storageDirProp, "generated");
        Files.createDirectories(dir);
        return dir;
    }

    public String generate(Agreement a) throws Exception {
        Path outPath = outputDir().resolve(safeName(a.getId()) + ".pdf");
        String fullHtml = wrap(a);

        byte[] pdf;
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFonts(builder);
            builder.withHtmlContent(fullHtml, null);
            builder.toStream(buffer);
            builder.run();
            pdf = buffer.toByteArray();
        }

        if (isDocMode(a)) {
            pdf = stampLetterhead(pdf);
        }

        try (FileOutputStream fos = new FileOutputStream(outPath.toFile())) {
            fos.write(pdf);
        }
        return outPath.toAbsolutePath().toString();
    }

    public void deleteQuietly(String path) {
        if (path == null) return;
        try { Files.deleteIfExists(Paths.get(path)); } catch (Exception ignored) { }
    }

    // ---------------------------------------------------------------- fonts
    private void registerFonts(PdfRendererBuilder b) {
        font(b, "LiberationSerif-Regular.ttf", "Liberation Serif", 400, FontStyle.NORMAL);
        font(b, "LiberationSerif-Bold.ttf", "Liberation Serif", 700, FontStyle.NORMAL);
        font(b, "LiberationSerif-Italic.ttf", "Liberation Serif", 400, FontStyle.ITALIC);
        font(b, "LiberationSerif-BoldItalic.ttf", "Liberation Serif", 700, FontStyle.ITALIC);
        font(b, "DejaVuSans.ttf", "DejaVu Sans", 400, FontStyle.NORMAL);
        font(b, "DejaVuSans-Bold.ttf", "DejaVu Sans", 700, FontStyle.NORMAL);
    }

    private void font(PdfRendererBuilder b, String file, String family, int weight, FontStyle style) {
        String path = "/fonts/" + file;
        if (getClass().getResource(path) == null) return;      // font missing -> fall back silently
        b.useFont(() -> getClass().getResourceAsStream(path), family, weight, style, true);
    }

    // ---------------------------------------------------------------- html
    private boolean isDocMode(Agreement a) {
        return a.getContentHtml() != null && a.getContentHtml().contains("id=\"agr-doc\"");
    }

    private String wrap(Agreement a) {
        String body = normalizeEntities(a.getContentHtml());
        if (isDocMode(a)) {
            // letterhead is stamped afterwards by stampLetterhead(); the @page margins just leave room for it.
            return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>" + DOC_CSS + "</style></head><body>"
                    + body + "</body></html>";
        }
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>" + CSS + "</style></head><body>"
                + normalizeEntities(a.getHeaderHtml()) + body + normalizeEntities(a.getFooterHtml()) + "</body></html>";
    }

    // ---------------------------------------------------------------- letterhead (PDFBox)

    private byte[] stampLetterhead(byte[] pdfBytes) {
        try (PDDocument doc = PDDocument.load(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ---- Super Admin branding (shared with quotations) ----
            byte[] padBytes = branding.letterpadBytes();   // uploaded letterpad, else bundled default (may be null)
            byte[] logoBytes = branding.logoBytes();       // uploaded logo (may be null)
            Map<String, Object> f = branding.footerSettings();

            PDImageXObject pad = imageFromBytes(doc, padBytes, "letterpad");
            PDImageXObject logo = imageFromBytes(doc, logoBytes, "logo");
            PDImageXObject iconPhone = imageFromBytes(doc, QuotationIcons.png("phone"), "icon-phone");
            PDImageXObject iconGlobe = imageFromBytes(doc, QuotationIcons.png("globe"), "icon-globe");
            PDImageXObject iconPin = imageFromBytes(doc, QuotationIcons.png("pin"), "icon-pin");

            // ---- legacy built-in assets, only used when the Super Admin has uploaded nothing ----
            PDImageXObject oldRibbonTop = pad == null ? classpathImage(doc, "ribbon-top.png") : null;
            PDImageXObject oldRibbonBottom = pad == null ? classpathImage(doc, "ribbon-bottom.png") : null;
            PDImageXObject oldLogo = logo == null ? classpathImage(doc, "logo.png") : null;

            for (PDPage page : doc.getPages()) {
                PDRectangle box = page.getMediaBox();
                float w = box.getWidth();
                float h = box.getHeight();

                // 1) letterpad goes BEHIND the text
                if (pad != null) {
                    try (PDPageContentStream bg = new PDPageContentStream(
                            doc, page, PDPageContentStream.AppendMode.PREPEND, true, true)) {
                        bg.drawImage(pad, 0f, 0f, w, h);
                    }
                }

                // 2) logo + footer on top
                try (PDPageContentStream cs = new PDPageContentStream(
                        doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    if (oldRibbonTop != null) drawOldRibbonTop(cs, w, h, oldRibbonTop);
                    if (oldRibbonBottom != null) drawOldRibbonBottom(cs, oldRibbonBottom);

                    float textX = 0.094f * w;
                    if (logo != null) {
                        textX = drawLogo(cs, w, h, logo);
                    } else if (oldLogo != null) {
                        drawOldBrandHeader(cs, h, oldLogo);
                    }
                    if (logo != null && Boolean.TRUE.equals(f.get("showBrandText"))) {
                        drawBrandText(cs, w, h, textX);
                    }

                    drawFooter(cs, w, h, iconPhone, iconGlobe, iconPin,
                            str(f.get("phone")), str(f.get("website")), str(f.get("address")));
                }
            }

            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("Letterhead stamping failed, returning PDF without letterhead: {}", e.getMessage(), e);
            return pdfBytes;
        }
    }

    /** Same box as quotations: x=9.4% , top=2.8%, max 46% x 10.5% of the page, centred vertically. Returns x for text after it. */
    private float drawLogo(PDPageContentStream cs, float w, float h, PDImageXObject logo) throws Exception {
        float boxW = 0.46f * w, boxH = 0.105f * h;
        float s = Math.min(boxW / logo.getWidth(), boxH / logo.getHeight());
        float lw = logo.getWidth() * s, lh = logo.getHeight() * s;
        float x = 0.094f * w;
        float topFromTop = 0.028f * h + (boxH - lh) / 2f;
        cs.drawImage(logo, x, h - topFromTop - lh, lw, lh);
        return x + lw + 0.02f * w;
    }

    private void drawBrandText(PDPageContentStream cs, float w, float h, float textX) throws Exception {
        text(cs, "CAMPAS TECH", PDType1Font.HELVETICA, 24f, BRAND, textX, h - (0.045f * h + 0.025f * h + 8f));
        text(cs, "Private Limited", PDType1Font.TIMES_ROMAN, 14f, BRAND, textX, h - (0.098f * h + 0.0175f * h + 5f));
    }

    /** Footer exactly like quotations: round icons + text, bottom-left. */
    private void drawFooter(PDPageContentStream cs, float w, float h,
                            PDImageXObject iconPhone, PDImageXObject iconGlobe, PDImageXObject iconPin,
                            String phone, String web, String address) throws Exception {
        float iconS = 19f;
        float ix = 0.14f * w, tx = 0.185f * w;
        PDFont font = PDType1Font.HELVETICA;
        float size = 10f;

        // icon (top y fraction) and text baseline derived from the same boxes used on quotations
        footerLine(cs, iconPhone, font, size, INK, ix, tx, iconS, h, 0.842f, 0.838f, phone);
        footerLine(cs, iconGlobe, font, size, LINK, ix, tx, iconS, h, 0.880f, 0.876f, web);

        if (iconPin != null) cs.drawImage(iconPin, ix, h - 0.914f * h - iconS, iconS, iconS);
        String[] lines = address.split("\\r?\\n");
        float firstBaseline = h - (0.910f * h + 1f + 9f);
        for (int i = 0; i < lines.length && i < 3; i++) {
            text(cs, lines[i], font, size, ADDR, tx, firstBaseline - i * 12f);
        }
    }

    private void footerLine(PDPageContentStream cs, PDImageXObject icon, PDFont font, float size, Color color,
                            float ix, float tx, float iconS, float h, float iconTopFrac, float textTopFrac,
                            String value) throws Exception {
        if (icon != null) cs.drawImage(icon, ix, h - iconTopFrac * h - iconS, iconS, iconS);
        float textBoxH = 0.028f * h;
        float baseline = h - (textTopFrac * h + textBoxH / 2f + size * 0.3f);
        text(cs, value, font, size, color, tx, baseline);
    }

    // ---- legacy (only when nothing has been uploaded) ----
    private void drawOldRibbonTop(PDPageContentStream cs, float w, float h, PDImageXObject img) throws Exception {
        float s = Math.min(34f * MM / img.getWidth(), 40f * MM / img.getHeight());
        float iw = img.getWidth() * s, ih = img.getHeight() * s;
        cs.drawImage(img, w - iw, h - ih, iw, ih);
    }

    private void drawOldRibbonBottom(PDPageContentStream cs, PDImageXObject img) throws Exception {
        float s = Math.min(36f * MM / img.getWidth(), 31f * MM / img.getHeight());
        cs.drawImage(img, 0f, 0f, img.getWidth() * s, img.getHeight() * s);
    }

    private void drawOldBrandHeader(PDPageContentStream cs, float h, PDImageXObject logo) throws Exception {
        float x = 10f * MM;
        float lh = 20f * MM;
        float lw = lh * logo.getWidth() / logo.getHeight();
        cs.drawImage(logo, x, h - 9f * MM - lh, lw, lh);

        PDFont bold = PDType1Font.TIMES_BOLD, reg = PDType1Font.TIMES_ROMAN;
        Color c = new Color(0x6e, 0x1f, 0x2b);
        float indoX = x + lw + 3f * MM;
        text(cs, "INDO", bold, 34f, c, indoX, h - 23f * MM);
        float campasX = indoX + bold.getStringWidth("INDO") / 1000f * 34f + 3f * MM;
        text(cs, "CAMPAS TECH", reg, 20f, c, campasX, h - 16.5f * MM);
        text(cs, "Private Limited", reg, 13f, c, campasX + 8f, h - 24f * MM);
    }

    // ---- helpers ----
    private void text(PDPageContentStream cs, String s, PDFont font, float size, Color color,
                      float x, float y) throws Exception {
        String safe = winAnsi(font, s);
        if (safe.isEmpty()) return;
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(safe);
        cs.endText();
    }

    /** Standard-14 fonts only support WinAnsi - replace anything they cannot encode so PDF never fails. */
    private String winAnsi(PDFont font, String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch == '\n' || ch == '\r') continue;
            try { font.encode(String.valueOf(ch)); sb.append(ch); } catch (Exception e) { sb.append('?'); }
        }
        return sb.toString();
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private PDImageXObject imageFromBytes(PDDocument doc, byte[] bytes, String name) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return PDImageXObject.createFromByteArray(doc, bytes, name);
        } catch (Exception e) {
            log.warn("Unable to load letterhead image {}: {}", name, e.getMessage());
            return null;
        }
    }

    private PDImageXObject classpathImage(PDDocument doc, String name) {
        try (InputStream in = getClass().getResourceAsStream("/agreement-brand/" + name)) {
            if (in == null) return null;
            return PDImageXObject.createFromByteArray(doc, in.readAllBytes(), name);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeName(String id) { return id == null ? "agreement" : id.replaceAll("[^a-zA-Z0-9_-]", "_"); }
    private String safe(String v) { return v == null ? "" : v; }

    private String normalizeEntities(String html) {
        return safe(html)
                .replace("&nbsp;", "&#160;").replace("&ldquo;", "&#8220;").replace("&rdquo;", "&#8221;")
                .replace("&lsquo;", "&#8216;").replace("&rsquo;", "&#8217;").replace("&ndash;", "&#8211;")
                .replace("&mdash;", "&#8212;").replace("&hellip;", "&#8230;");
    }

    // Margins leave room for the quotation letterhead: logo ends ~39.5mm from top, footer starts ~249mm from top.
    private static final String DOC_CSS = """
        @page { size: A4; margin: 42mm 20mm 52mm 20mm; }
        body { font-family: 'Liberation Serif', 'Times New Roman', serif; font-size: 11pt; color: #111; }
        h3, h4 { page-break-after: avoid; }
        li, p { page-break-inside: avoid; }
        """;

    private static final String CSS = """
        @page { size: A4; margin: 22mm 18mm; }
        body { font-family: 'Liberation Serif', 'Times New Roman', serif; font-size: 11.5pt; color: #1a1a1a; line-height: 1.45; }
        .agreement-letterhead { border-bottom: 2px solid #8a1f2b; padding-bottom: 8px; margin-bottom: 16px; }
        .agreement-brand { font-size: 20pt; font-weight: bold; color: #8a1f2b; }
        .agreement-brand span { font-size: 12pt; color: #444; display: block; font-weight: normal; }
        .agreement-contact { font-size: 9pt; color: #444; margin-top: 4px; }
        .agreement-title { text-align: center; font-size: 14pt; margin: 10px 0 18px; }
        h3 { font-size: 12.5pt; margin: 14px 0 6px; }
        h4 { font-size: 11.5pt; margin: 10px 0 4px; }
        p { margin: 4px 0; text-align: justify; }
        ul { margin: 4px 0 8px 20px; }
        li { margin: 2px 0; }
        table.agreement-fee-table { border-collapse: collapse; width: 100%; margin: 10px 0 14px; }
        table.agreement-fee-table td { border: 1px solid #999; padding: 6px 10px; }
        table.agreement-fee-table caption { text-align: left; font-weight: bold; margin-bottom: 6px; }
        .agreement-signatures { width: 100%; margin-top: 40px; }
        .agreement-signature-block { display: inline-block; vertical-align: top; width: 46%; }
        .agreement-signature-block + .agreement-signature-block { margin-left: 4%; }
        .agreement-signature-block p { margin: 2px 0; }
        """;
}