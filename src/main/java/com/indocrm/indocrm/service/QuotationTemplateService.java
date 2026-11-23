package com.indocrm.indocrm.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indocrm.indocrm.model.QuotationTemplate;
import com.indocrm.indocrm.model.QuotationTemplate.DesignBlock;
import com.indocrm.indocrm.model.QuotationTemplate.DesignPage;
import com.indocrm.indocrm.model.QuotationTemplate.TemplateDesign;
import com.indocrm.indocrm.model.QuotationTemplate.TemplateField;
import com.indocrm.indocrm.repository.QuotationTemplateRepository;

@Service
public class QuotationTemplateService {

    private final QuotationTemplateRepository repo;
    private final QuotationLayouts layouts;

    // true  -> only the built-in IndoCampas templates are offered (default)
    // false -> previously uploaded / designed templates are listed as well (only if their files still exist)
    @Value("${quotation.templates.builtin-only:true}")
    private boolean builtinOnly;

    @Value("${quotation.storage.dir:./storage/quotations}")
    private String storageDirProp;

    private static final double A4_W = 595.28;
    private static final double A4_H = 841.89;
    private static final int PREVIEW_RENDER_DPI = 150;

    // ── IndoCampas Tech branded letterhead assets ─────────────────────────
    // Logo, ribbon corner graphics and contact icons extracted from the
    // company's approved letterhead (template_bb.docx) and embedded as
    // base64 data URLs so every new quotation template starts pre-branded
    // and fully editable in the drag-and-drop designer (nothing here is
    // baked into the PDF as a flat image — each element is its own movable,
    // resizable, restylable block/field).
    private static volatile Map<String, String> BRAND_ASSETS;

    private static Map<String, String> brandAssets() {
        if (BRAND_ASSETS != null) return BRAND_ASSETS;
        synchronized (QuotationTemplateService.class) {
            if (BRAND_ASSETS != null) return BRAND_ASSETS;
            Map<String, String> assets = new HashMap<>();
            try (InputStream in = QuotationTemplateService.class
                    .getResourceAsStream("/quotation-templates/indocampas-assets.json")) {
                if (in != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> parsed = new ObjectMapper().readValue(in, Map.class);
                    assets = parsed;
                }
            } catch (Exception ignored) {
                // Missing/unreadable asset bundle just means the default template
                // falls back to plain text blocks with no logo/ribbon graphics.
            }
            BRAND_ASSETS = assets;
            return BRAND_ASSETS;
        }
    }

    private static String asset(String key) {
        return brandAssets().getOrDefault(key, "");
    }

    public QuotationTemplateService(QuotationTemplateRepository repo, QuotationLayouts layouts) {
        this.repo = repo;
        this.layouts = layouts;
    }

    /** Letterhead (logo, ribbons, contact strip) used by the built-in layouts. */
    public List<DesignBlock> letterheadBlocks() {
        return indoCampasLetterheadBlocks();
    }

    /** A stored template is usable only if every file it needs is still on disk. */
    private boolean isUsable(QuotationTemplate t) {
        if (t == null) return false;
        String mode = t.getDesign() == null ? null : t.getDesign().getMode();
        boolean background = mode == null || "BACKGROUND".equalsIgnoreCase(mode);
        if (!background) return true;
        String path = t.getStoredRenderPdfPath() != null && !t.getStoredRenderPdfPath().isBlank()
                ? t.getStoredRenderPdfPath() : t.getStoredOriginalPath();
        return path != null && new File(path).isFile();
    }

    /** Template to use when generating a quotation PDF. Never fails because of a missing file:
     *  anything unknown or broken falls back to the built-in layout for the service type. */
    public QuotationTemplate resolveForQuotation(String templateId, String serviceType) {
        if (QuotationLayouts.isBuiltIn(templateId)) return layouts.byId(templateId);
        if (!builtinOnly && templateId != null && !templateId.isBlank()) {
            QuotationTemplate t = repo.findById(templateId).orElse(null);
            if (isUsable(t)) return t;
        }
        return layouts.forServiceType(serviceType);
    }

    private Path storageDir() throws Exception {
        Path dir = Paths.get(storageDirProp, "templates");
        Files.createDirectories(dir);
        return dir;
    }

    public List<QuotationTemplate> list(String username) {
        List<QuotationTemplate> out = new ArrayList<>(layouts.builtInTemplates());
        if (!builtinOnly) {
            for (QuotationTemplate t : repo.findByCreatedByOrderByCreatedAtDesc(username)) {
                if (isUsable(t)) out.add(t);
            }
        }
        return out;
    }

    public QuotationTemplate get(String id) {
        if (QuotationLayouts.isBuiltIn(id)) return layouts.byId(id);
        QuotationTemplate t = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
        return t;
    }

    public QuotationTemplate createBlank(String name, String username) throws Exception {
        QuotationTemplate t = new QuotationTemplate();
        t.setName(name == null || name.isBlank() ? "New Quotation Template" : name.trim());
        t.setCreatedBy(username);
        t.setPdfSource(false);
        t.setPageWidthPt(A4_W);
        t.setPageHeightPt(A4_H);
        t.setFields(new ArrayList<>());
        t.setDesign(defaultDesign());
        return repo.save(t);
    }

    public QuotationTemplate upload(MultipartFile file, String name, String username) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("No file uploaded.");
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        boolean isPdf = contentType.equalsIgnoreCase("application/pdf")
                || (file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".pdf"));

        Path dir = storageDir();
        String uid = UUID.randomUUID().toString();
        String safeOriginalName = (file.getOriginalFilename() == null ? "template" : file.getOriginalFilename())
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        Path originalPath = dir.resolve(uid + "_" + safeOriginalName);
        Path previewPath = dir.resolve(uid + "_preview.png");

        QuotationTemplate template = new QuotationTemplate();
        template.setName(name != null && !name.isBlank() ? name : safeOriginalName);
        template.setOriginalFileName(safeOriginalName);
        template.setOriginalContentType(contentType);
        template.setCreatedBy(username);
        template.setPdfSource(isPdf);
        template.setDesign(defaultBackgroundDesign());

        Files.write(originalPath, file.getBytes());

        if (isPdf) {
            try (PDDocument doc = PDDocument.load(originalPath.toFile())) {
                PDPage page = doc.getPage(0);
                double widthPt = page.getMediaBox().getWidth();
                double heightPt = page.getMediaBox().getHeight();
                PDFRenderer renderer = new PDFRenderer(doc);
                BufferedImage preview = renderer.renderImageWithDPI(0, PREVIEW_RENDER_DPI);
                ImageIO.write(preview, "png", previewPath.toFile());
                template.setPageWidthPt(widthPt);
                template.setPageHeightPt(heightPt);
                template.setPreviewWidthPx(preview.getWidth());
                template.setPreviewHeightPx(preview.getHeight());
            }
        } else {
            BufferedImage img = ImageIO.read(originalPath.toFile());
            if (img == null) throw new IllegalArgumentException("Uploaded file is not a readable image or PDF.");
            ImageIO.write(img, "png", previewPath.toFile());
            double widthPt = A4_W;
            double heightPt = widthPt * ((double) img.getHeight() / (double) img.getWidth());
            template.setPageWidthPt(widthPt);
            template.setPageHeightPt(heightPt);
            template.setPreviewWidthPx(img.getWidth());
            template.setPreviewHeightPx(img.getHeight());
        }

        template.setStoredOriginalPath(originalPath.toAbsolutePath().toString());
        template.setStoredPreviewPath(previewPath.toAbsolutePath().toString());
        return repo.save(template);
    }

    public QuotationTemplate saveFields(String id, List<TemplateField> fields) {
        QuotationTemplate template = get(id);
        template.setFields(fields == null ? new ArrayList<>() : fields);
        return repo.save(template);
    }

    public QuotationTemplate saveDesign(String id, String name, TemplateDesign design, List<TemplateField> fields) {
        QuotationTemplate template = get(id);
        if (name != null && !name.isBlank()) template.setName(name.trim());
        template.setDesign(design == null ? defaultDesign() : design);
        if (template.getDesign().getPages() != null && !template.getDesign().getPages().isEmpty()) {
            template.getDesign().setBlocks(template.getDesign().getPages().get(0).getBlocks());
        }
        if (fields != null) template.setFields(fields);

        if ("BUILDER".equalsIgnoreCase(template.getDesign().getMode())) {
            template.setPdfSource(false);
            if (template.getPageWidthPt() <= 0) template.setPageWidthPt(A4_W);
            if (template.getPageHeightPt() <= 0) template.setPageHeightPt(A4_H);
        }
        return repo.save(template);
    }

    public File previewFile(String id) {
        QuotationTemplate template = get(id);
        if (template.getStoredPreviewPath() == null) return new File("__missing_preview__");
        return new File(template.getStoredPreviewPath());
    }

    public void delete(String id, String username) {
        QuotationTemplate template = get(id);
        if (!username.equals(template.getCreatedBy())) {
            throw new org.springframework.security.access.AccessDeniedException("Template access denied");
        }
        try {
            if (template.getStoredOriginalPath() != null) Files.deleteIfExists(Paths.get(template.getStoredOriginalPath()));
            if (template.getStoredPreviewPath() != null) Files.deleteIfExists(Paths.get(template.getStoredPreviewPath()));
        } catch (Exception ignored) {}
        repo.deleteById(id);
    }

    public TemplateDesign defaultDesign() {
        TemplateDesign d = new TemplateDesign();
        d.setMode("BUILDER");
        d.setCompanyName("INDO CAMPAS TECH");
        d.setCompanySubtitle("Private Limited");
        d.setCompanyPhone("9944213425 / 7339547987");
        d.setCompanyEmail("indocampasoffice01@gmail.com");
        d.setCompanyWebsite("www.indocampas.com");
        d.setCompanyAddress("TIDEL NEO - Thiruchitrambalam, Koot Road, Tamil Nadu 605111");
        d.setPrimaryColor("#7B1113");
        d.setSecondaryColor("#172033");
        d.setAccentColor("#DC2626");
        d.setTextColor("#111827");
        d.setHeadingColor("#7B1113");
        d.setBackgroundColor("#FFFFFF");
        // The IndoCampas letterhead is a white page with logo/ribbon artwork in
        // the corners rather than a full-width coloured band, so the built-in
        // header/footer bars are switched off and the letterhead is composed
        // entirely from editable blocks instead (see below).
        d.setHeaderEnabled(false);
        d.setFooterEnabled(false);
        d.setHeaderHeight(0.16);
        d.setFooterHeight(0.12);

        List<DesignBlock> page1Blocks = indoCampasLetterheadBlocks();
        page1Blocks.addAll(List.of(
            block("intro", "text", 0.08, 0.205, 0.86, 0.075,
                "Dear Sir / Madam,\nThank you for your interest in our services. As discussed, please find below the quotation details for our ERP software solutions, admin dashboard access, and digital marketing services.",
                "", 10.5, "#111827", "transparent", "left", false),
            block("prepFor", "text", 0.08, 0.285, 0.86, 0.03,
                "Prepared for: {{clientName}} ({{organization}})   |   Subject: {{subject}}   |   Valid Till: {{validTill}}",
                "", 9.5, "#374151", "#F9FAFB", "left", false),
            block("detailsHeading", "heading", 0.08, 0.33, 0.86, 0.04,
                "Quotation Details", "", 15, "#7B1113", "transparent", "left", true),

            block("h1", "heading", 0.08, 0.375, 0.86, 0.028,
                "1. Admin Dashboard", "", 12, "#7B1113", "transparent", "left", true),
            block("b1", "text", 0.08, 0.403, 0.86, 0.025,
                "• Admin Dashboard – NO COST", "", 10.5, "#111827", "transparent", "left", false),

            block("h2", "heading", 0.08, 0.44, 0.86, 0.028,
                "2. Digital Marketing Services", "", 12, "#7B1113", "transparent", "left", true),
            block("b2price", "text", 0.08, 0.468, 0.86, 0.025,
                "• Digital Marketing Package – ₹16,000 + GST", "", 10.5, "#111827", "transparent", "left", false),
            block("b2list", "text", 0.08, 0.495, 0.86, 0.115,
                "Includes:\n• Meta Ads Campaign Management\n• Facebook Ads\n• Instagram Ads\n• WhatsApp Promotions\n• YouTube Ads\n• Online visibility & lead generation support",
                "", 10, "#111827", "transparent", "left", false),
            block("b2note", "text", 0.08, 0.615, 0.86, 0.035,
                "(This package helps improve your institution's digital presence and reach more prospective leads.)",
                "", 9, "#4B5563", "transparent", "left", false),

            block("h3", "heading", 0.08, 0.655, 0.86, 0.045,
                "3. ERP Software Packages (Entire School Management – 3 logins for Admin Dashboard, Faculty Login, Parent Login)",
                "", 11.5, "#7B1113", "transparent", "left", true),
            block("b3", "text", 0.08, 0.705, 0.86, 0.07,
                "• Pro Plan: ₹10,000\n• Pro Plus Plan: ₹18,000\n• Pro Max Plan: ₹28,000\n(All prices are exclusive of GST, unless mentioned otherwise.)",
                "", 10.5, "#111827", "transparent", "left", false)
        ));

        List<DesignBlock> page2Blocks = indoCampasLetterheadBlocks();
        page2Blocks.addAll(List.of(
            block("fees", "heading", 0.08, 0.22, 0.86, 0.04,
                "4. Fees and Payments", "", 14, "#7B1113", "transparent", "left", true),
            block("feesBody", "text", 0.08, 0.27, 0.86, 0.10,
                "Fees Structure:\n• The Institution agrees to pay the Service Provider: Rs. 2000/- per student admitted through the platform.\n• However, if you purchase the ERP Software from us, then for admissions received through the dashboard, you only need to pay ₹1000 per admission.",
                "", 10.5, "#111827", "transparent", "left", false),
            block("terms", "heading", 0.08, 0.40, 0.86, 0.035,
                "Notes", "", 13, "#7B1113", "transparent", "left", true),
            block("notes2", "text", 0.08, 0.44, 0.86, 0.11,
                "{{notes}}\n• GST will be charged as applicable.\n• Pricing is valid for a limited period.\n• Final scope and implementation will be confirmed upon approval.",
                "notes", 10, "#111827", "#F9FAFB", "left", false),
            block("closing", "text", 0.08, 0.575, 0.86, 0.05,
                "Please feel free to contact us if you need any clarification or customization. We look forward to working with you.",
                "", 10.5, "#111827", "transparent", "center", false),
            block("signature2", "signature", 0.55, 0.65, 0.37, 0.10,
                "Warm regards,\n{{companyName}}\n{{companyPhone}}\n{{companyEmail}}",
                "", 10.5, "#111827", "transparent", "left", false)
        ));

        d.setBlocks(page1Blocks);

        List<DesignPage> pages = new ArrayList<>();
        DesignPage page1 = new DesignPage();
        page1.setId("page_1");
        page1.setName("Page 1");
        page1.setBackgroundColor(d.getBackgroundColor());
        page1.setHeaderEnabled(false);
        page1.setFooterEnabled(false);
        page1.setBlocks(page1Blocks);

        DesignPage page2 = new DesignPage();
        page2.setId("page_2");
        page2.setName("Page 2");
        page2.setBackgroundColor(d.getBackgroundColor());
        page2.setHeaderEnabled(false);
        page2.setFooterEnabled(false);
        page2.setBlocks(page2Blocks);

        pages.add(page1);
        pages.add(page2);
        d.setPages(pages);
        return d;
    }

    /** The IndoCampas Tech letterhead — logo, ribbon corner graphics and the
     *  contact strip — as a fresh, independently-editable set of blocks. Every
     *  call returns new block instances so page 1 and page 2 don't share (and
     *  therefore can't accidentally cross-mutate) the same objects. */
    private List<DesignBlock> indoCampasLetterheadBlocks() {
        List<DesignBlock> blocks = new ArrayList<>();

        // Top-right ribbon artwork
        DesignBlock ribbonTop = image("ribbonTop", 0.70, 0.0, 0.30, 0.19, asset("ribbonTop"));
        blocks.add(ribbonTop);

        // Logo mark (icon + "INDO" wordmark) top-left
        blocks.add(image("logoMark", 0.035, 0.035, 0.30, 0.11, asset("wordmark")));
        // Live, editable brand text next to the logo mark
        blocks.add(block("brandName", "text", 0.335, 0.045, 0.35, 0.055,
                "CAMPAS TECH", "", 21, "#7B1113", "transparent", "left", true));
        blocks.add(block("brandSubtitle", "text", 0.335, 0.10, 0.35, 0.035,
                "Private Limited", "", 12.5, "#7B1113", "transparent", "left", false));

        // Bottom-left ribbon artwork
        blocks.add(image("ribbonBottom", 0.0, 0.80, 0.17, 0.20, asset("ribbonBottom")));

        // Contact strip (icon + text rows) sitting above the bottom ribbon
        blocks.add(image("phoneIcon", 0.125, 0.845, 0.032, 0.028, asset("phoneIcon")));
        blocks.add(block("phoneText", "text", 0.17, 0.845, 0.55, 0.028,
                "{{companyPhone}}", "companyPhone", 9.5, "#111827", "transparent", "left", false));

        blocks.add(image("globeIcon", 0.125, 0.883, 0.032, 0.028, asset("globeIcon")));
        blocks.add(block("websiteText", "text", 0.17, 0.883, 0.55, 0.028,
                "{{companyWebsite}}", "companyWebsite", 9.5, "#1D4ED8", "transparent", "left", false));

        blocks.add(image("pinIcon", 0.125, 0.918, 0.032, 0.028, asset("pinIcon")));
        blocks.add(block("addressText", "text", 0.17, 0.915, 0.60, 0.05,
                "{{companyAddress}}", "companyAddress", 9.5, "#111827", "transparent", "left", false));

        return blocks;
    }

    private DesignBlock image(String id, double x, double y, double w, double h, String imageDataUrl) {
        DesignBlock b = new DesignBlock();
        b.setId(id);
        b.setType("image");
        b.setX(x); b.setY(y); b.setWidth(w); b.setHeight(h);
        b.setImageDataUrl(imageDataUrl == null ? "" : imageDataUrl);
        return b;
    }


    private TemplateDesign defaultBackgroundDesign() {
        TemplateDesign d = defaultDesign();
        d.setMode("BACKGROUND");
        d.setHeaderEnabled(false);
        d.setFooterEnabled(false);
        d.setBlocks(new ArrayList<>());
        return d;
    }

    private DesignBlock block(String id, String type, double x, double y, double w, double h,
                              String text, String mapsTo, double fontSize, String color,
                              String bg, String align, boolean bold) {
        DesignBlock b = new DesignBlock();
        b.setId(id);
        b.setType(type);
        b.setX(x); b.setY(y); b.setWidth(w); b.setHeight(h);
        b.setText(text); b.setMapsTo(mapsTo); b.setFontSize(fontSize);
        b.setColor(color); b.setBackgroundColor(bg); b.setAlign(align); b.setBold(bold);
        b.setFontFamily("Helvetica");
        b.setPadding(4);
        return b;
    }
}