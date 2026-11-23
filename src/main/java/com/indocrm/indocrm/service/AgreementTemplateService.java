// FILE: src/main/java/com/indocrm/indocrm/service/AgreementTemplateService.java
// REPLACES existing file.
//
// FIX: the old createSystemTemplate(name, contentHtml, type) was being called
// by the bootstrap as createSystemTemplate(name, "PDF", <html>) — i.e. the
// literal string "PDF" was stored as contentHtml, and the real HTML template
// ended up in the "type" field. Every agreement generated from either system
// template therefore rendered as just the word "PDF". This version fixes the
// signature/call order AND makes system-template installs version-aware, so
// the corrected content actually reaches existing installs on next boot
// instead of being skipped by the old "never overwrite" guard.
package com.indocrm.indocrm.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.indocrm.indocrm.model.AgreementTemplate;
import com.indocrm.indocrm.repository.AgreementTemplateRepository;

@Service
public class AgreementTemplateService {

    private final AgreementTemplateRepository repo;
    private final TelecallerAccessService access;

    public AgreementTemplateService(
            AgreementTemplateRepository r,
            TelecallerAccessService a
    ) {
        repo = r;
        access = a;
    }

    public List<AgreementTemplate> list() {
        return repo.findByActiveTrueOrderByCreatedAtDesc();
    }

    /** Templates for one agreement flow — "ERP" or "DIGITAL_MARKETING". */
    public List<AgreementTemplate> listByType(String agreementType) {
        if (agreementType == null || agreementType.isBlank()) return list();
        return repo.findByAgreementTypeAndActiveTrueOrderByCreatedAtDesc(agreementType.trim().toUpperCase());
    }

    public AgreementTemplate get(String id) {
        return repo.findById(id)
                .filter(AgreementTemplate::isActive)
                .orElseThrow(() ->
                        new IllegalArgumentException("Template not found"));
    }

    /**
     * Creates a normal HTML template manually.
     */
    public AgreementTemplate create(
            String name,
            String contentHtml
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Template name is required"
            );
        }

        if (contentHtml == null || contentHtml.isBlank()) {
            throw new IllegalArgumentException(
                    "Template content is required"
            );
        }

        AgreementTemplate t = new AgreementTemplate();

        t.setName(name.trim());
        t.setContentHtml(contentHtml);
        t.setContentType("text/html");
        t.setSourceType("HTML");

        t.setCreatedBy(access.currentUsername());
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());

        return repo.save(t);
    }

    /**
     * Creates (or version-upgrades) a system/default template.
     *
     * Used only by AgreementTemplateBootstrapService on startup.
     *
     * Behaviour:
     *  - No template with this name yet            -> create it fresh.
     *  - Template exists, is still a system template,
     *    and its stored templateVersion is OLDER
     *    than `version`                            -> overwrite contentHtml/
     *                                                  agreementType/version
     *                                                  (self-heals bad
     *                                                  installs like the
     *                                                  "PDF" bug above).
     *  - Template exists but an admin has edited it
     *    by hand (systemTemplate flipped to false,
     *    e.g. via the template editor UI)           -> left untouched.
     *  - Template exists and is already at/above
     *    `version`                                  -> left untouched.
     *
     * @param name          Template name (also the upsert key)
     * @param agreementType "ERP" or "DIGITAL_MARKETING"
     * @param version       Bump this whenever contentHtml below changes
     * @param contentHtml   The HTML content
     */
    public AgreementTemplate createSystemTemplate(
            String name,
            String agreementType,
            int version,
            String contentHtml
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("System template name is required");
        }
        if (contentHtml == null || contentHtml.isBlank()) {
            throw new IllegalArgumentException("System template content is required");
        }

        String templateName = name.trim();

        Optional<AgreementTemplate> existing = repo.findByNameIgnoreCase(templateName);

        if (existing.isPresent()) {
            AgreementTemplate t = existing.get();

            boolean handEdited = !t.isSystemTemplate();
            boolean upToDate = t.getTemplateVersion() >= version;

            if (handEdited || upToDate) {
                return t;
            }

            t.setContentHtml(contentHtml);
            t.setAgreementType(agreementType);
            t.setTemplateVersion(version);
            t.setContentType("text/html");
            t.setSourceType("SYSTEM");
            t.setActive(true);
            t.setUpdatedAt(Instant.now());
            return repo.save(t);
        }

        AgreementTemplate t = new AgreementTemplate();
        t.setName(templateName);
        t.setContentHtml(contentHtml);
        t.setContentType("text/html");
        t.setSourceType("SYSTEM");
        t.setAgreementType(agreementType);
        t.setTemplateVersion(version);
        t.setSystemTemplate(true);
        t.setCreatedBy("SYSTEM");
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        return repo.save(t);
    }

    /**
     * Uploads a template file.
     */
    public AgreementTemplate upload(
            String name,
            String contentHtml,
            MultipartFile file
    ) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Template file is required"
            );
        }

        String fn = Optional
                .ofNullable(file.getOriginalFilename())
                .orElse("");

        String ext = fn.contains(".")
                ? fn.substring(fn.lastIndexOf('.')).toLowerCase()
                : "";

        if (!Set.of(
                ".doc",
                ".docx",
                ".pdf",
                ".jpg",
                ".jpeg",
                ".png"
        ).contains(ext)) {

            throw new IllegalArgumentException(
                    "Only DOC, DOCX, PDF, JPG, JPEG and PNG templates are supported"
            );
        }

        if (file.getSize() > 10L * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "Template maximum size is 10 MB"
            );
        }

        AgreementTemplate t = new AgreementTemplate();

        t.setName(
                name == null || name.isBlank()
                        ? fn
                        : name.trim()
        );

        t.setFileName(fn);
        t.setContentType(file.getContentType());

        t.setSourceType(
                ext.substring(1).toUpperCase()
        );

        t.setContentHtml(
                contentHtml == null
                        ? ""
                        : contentHtml
        );

        t.setOriginalFile(file.getBytes());

        t.setCreatedBy(
                access.currentUsername()
        );

        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());

        return repo.save(t);
    }

    /**
     * Updates HTML content.
     */
    public AgreementTemplate updateHtml(
            String id,
            String html
    ) {

        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException(
                    "Template content is required"
            );
        }

        AgreementTemplate t = get(id);

        t.setContentHtml(html);
        // A manual edit means the bootstrap must never overwrite this again.
        t.setSystemTemplate(false);
        t.setUpdatedAt(Instant.now());

        return repo.save(t);
    }

    /**
     * Renames a template.
     */
    public AgreementTemplate rename(
            String id,
            String name
    ) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Template name is required"
            );
        }

        AgreementTemplate t = get(id);

        t.setName(name.trim());
        t.setUpdatedAt(Instant.now());

        return repo.save(t);
    }

    /**
     * Soft delete / archive template.
     */
    public void archive(String id) {

        AgreementTemplate t = get(id);

        t.setActive(false);
        t.setUpdatedAt(Instant.now());

        repo.save(t);
    }
}
