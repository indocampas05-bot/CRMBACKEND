package com.indocrm.indocrm.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.indocrm.indocrm.model.InvoiceAsset;
import com.indocrm.indocrm.repository.InvoiceAssetRepository;

@Service
public class InvoiceAssetService {

    private static final Set<String> TYPES = Set.of("logo", "signature", "thank-you");
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final InvoiceAssetRepository repo;
    private final TelecallerAccessService access;

    public InvoiceAssetService(InvoiceAssetRepository repo, TelecallerAccessService access) {
        this.repo = repo;
        this.access = access;
    }

    public InvoiceAsset save(String type, MultipartFile file) throws IOException {
        if (type == null || !TYPES.contains(type))
            throw new IllegalArgumentException("Unknown invoice asset type: " + type);
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Please select an image first.");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/"))
            throw new IllegalArgumentException("Only image files are allowed.");
        if (file.getSize() > MAX_BYTES)
            throw new IllegalArgumentException("Image must be 5 MB or smaller.");

        repo.deleteByType(type); // only one active image per type

        InvoiceAsset a = new InvoiceAsset();
        a.setType(type);
        a.setFileName(file.getOriginalFilename());
        a.setContentType(ct);
        a.setData(file.getBytes());
        a.setUploadedAt(Instant.now());
        a.setUploadedBy(access.currentUsername());
        return repo.save(a);
    }

    /** Shape expected by the React invoice editor / PDF printer. */
    public Map<String, String> dataUrls() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("logoDataUrl", dataUrl("logo"));
        out.put("signatureDataUrl", dataUrl("signature"));
        out.put("thankYouDataUrl", dataUrl("thank-you"));
        return out;
    }

    private String dataUrl(String type) {
        return repo.findFirstByTypeOrderByUploadedAtDesc(type)
                .map(a -> "data:" + a.getContentType() + ";base64,"
                        + Base64.getEncoder().encodeToString(a.getData()))
                .orElse("");
    }
}