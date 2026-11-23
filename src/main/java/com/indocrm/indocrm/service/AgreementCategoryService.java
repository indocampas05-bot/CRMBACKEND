// FILE: src/main/java/com/indocrm/indocrm/service/AgreementCategoryService.java
// NEW FILE — one editable agreement template per category.
package com.indocrm.indocrm.service;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

import com.indocrm.indocrm.model.AgreementCategory;
import com.indocrm.indocrm.model.AgreementTemplate;
import com.indocrm.indocrm.repository.AgreementTemplateRepository;

@Service
public class AgreementCategoryService {

    /** Bump when you change a file under resources/agreement-templates/ — un-edited system copies self-heal on boot. */
    static final int DEFAULT_VERSION = 1;
    private static final int MAX_HTML_CHARS = 1_500_000;

    private final AgreementTemplateRepository repo;

    public AgreementCategoryService(AgreementTemplateRepository repo) {
        this.repo = repo;
    }

    // ------------------------------------------------------------------ read

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (AgreementCategory c : AgreementCategory.values()) {
            out.add(toMap(c, find(c), false));
        }
        return out;
    }

    public Map<String, Object> get(String key) {
        AgreementCategory c = AgreementCategory.of(key);
        return toMap(c, find(c), true);
    }

    // ----------------------------------------------------------------- write

    /** Saves the edited agreement. A hand-edited template is never overwritten by the boot seeder again. */
    public Map<String, Object> save(String key, String html) {
        AgreementCategory c = AgreementCategory.of(key);
        validate(html);
        AgreementTemplate t = find(c);
        if (t == null) t = create(c, defaultHtml(c));
        t.setContentHtml(html);
        t.setSystemTemplate(false);          // customised → protect from the seeder
        t.setUpdatedAt(Instant.now());
        return toMap(c, repo.save(t), true);
    }

    /** Restores the original agreement text (from the PDF the company supplied). */
    public Map<String, Object> reset(String key) {
        AgreementCategory c = AgreementCategory.of(key);
        AgreementTemplate t = find(c);
        if (t == null) t = create(c, defaultHtml(c));
        t.setContentHtml(defaultHtml(c));
        t.setSystemTemplate(true);
        t.setTemplateVersion(DEFAULT_VERSION);
        t.setUpdatedAt(Instant.now());
        return toMap(c, repo.save(t), true);
    }

    // ------------------------------------------------------------------ boot

    public void seedAll() {
        for (AgreementCategory c : AgreementCategory.values()) {
            AgreementTemplate t = find(c);
            if (t == null) {
                create(c, defaultHtml(c));
            } else if (t.isSystemTemplate() && t.getTemplateVersion() < DEFAULT_VERSION) {
                t.setContentHtml(defaultHtml(c));
                t.setTemplateVersion(DEFAULT_VERSION);
                t.setUpdatedAt(Instant.now());
                repo.save(t);
            }
        }
    }

    // --------------------------------------------------------------- helpers

    private AgreementTemplate find(AgreementCategory c) {
        return repo.findFirstByCategoryAndActiveTrue(c.name()).orElse(null);
    }

    private AgreementTemplate create(AgreementCategory c, String html) {
        AgreementTemplate t = new AgreementTemplate();
        t.setName(c.label + " Agreement");
        t.setCategory(c.name());
        t.setAgreementType(c.agreementType);
        t.setContentHtml(html);
        t.setContentType("text/html");
        t.setSourceType("SYSTEM");
        t.setSystemTemplate(true);
        t.setTemplateVersion(DEFAULT_VERSION);
        t.setCreatedBy("SYSTEM");
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        return repo.save(t);
    }

    private String defaultHtml(AgreementCategory c) {
        try (InputStream in = new ClassPathResource(c.resource).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Default agreement file missing: " + c.resource, e);
        }
    }

    /**
     * Agreement PDFs are rendered by openhtmltopdf, which parses the HTML as strict XML.
     * Reject anything that would later break PDF generation instead of discovering it at approval time.
     */
    private void validate(String html) {
        if (html == null || html.isBlank()) throw new IllegalArgumentException("Agreement content cannot be empty");
        if (html.length() > MAX_HTML_CHARS) throw new IllegalArgumentException("Agreement content is too large");
        String lower = html.toLowerCase();
        if (lower.contains("<script") || lower.contains("javascript:") || lower.matches("(?s).*\\son\\w+\\s*=.*")) {
            throw new IllegalArgumentException("Scripts and inline event handlers are not allowed in an agreement");
        }
        String xml = "<root>" + html
                .replace("&nbsp;", "&#160;").replace("&ldquo;", "&#8220;").replace("&rdquo;", "&#8221;")
                .replace("&lsquo;", "&#8216;").replace("&rsquo;", "&#8217;").replace("&ndash;", "&#8211;")
                .replace("&mdash;", "&#8212;").replace("&hellip;", "&#8230;") + "</root>";
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Agreement HTML is not well-formed (" + e.getMessage() + "). "
                    + "Close every tag, e.g. <br/> instead of <br>.");
        }
    }

    private Map<String, Object> toMap(AgreementCategory c, AgreementTemplate t, boolean withHtml) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", c.name());
        m.put("label", c.label);
        m.put("description", c.description);
        m.put("agreementType", c.agreementType);
        m.put("fields", c.fieldSet.fields);
        m.put("templateId", t == null ? null : t.getId());
        m.put("customized", t != null && !t.isSystemTemplate());
        m.put("updatedAt", t == null ? null : t.getUpdatedAt());
        if (withHtml) m.put("contentHtml", t == null ? defaultHtml(c) : t.getContentHtml());
        return m;
    }
}
