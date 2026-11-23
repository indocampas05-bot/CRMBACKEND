package com.indocrm.indocrm.service;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.indocrm.indocrm.model.Quotation;
import com.indocrm.indocrm.model.QuotationTemplate;

@Service
public class QuotationWhatsAppService {
    private final QuotationPdfService pdfService;
    private final QuotationTemplateService templateService;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${whatsapp.cloud-api.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.cloud-api.access-token:}")
    private String accessToken;

    @Value("${whatsapp.cloud-api.version:v23.0}")
    private String graphVersion;

    public QuotationWhatsAppService(QuotationPdfService pdfService,
                                    QuotationTemplateService templateService,
                                    ObjectMapper objectMapper) {
        this.pdfService = pdfService;
        this.templateService = templateService;
        this.objectMapper = objectMapper;
    }

    public void send(Quotation quotation, String recipient, String message) {
        if (phoneNumberId == null || phoneNumberId.isBlank() || accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("WhatsApp PDF sending is not configured. Add whatsapp.cloud-api.phone-number-id and whatsapp.cloud-api.access-token to the backend configuration.");
        }

        String to = normalizeIndianOrInternationalPhone(recipient);
        if (quotation.getTemplateId() == null || quotation.getTemplateId().isBlank()) {
            throw new IllegalStateException("Quotation does not have a templateId.");
        }

        QuotationTemplate template = templateService.resolveForQuotation(quotation.getTemplateId(), quotation.getServiceType());
        String pdfPath;
        try {
            pdfPath = pdfService.generate(quotation, template);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate quotation PDF: " + e.getMessage(), e);
        }

        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists() || !pdfFile.isFile()) {
            throw new IllegalStateException("Generated quotation PDF does not exist: " + pdfPath);
        }

        try {
            byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());
            String fileName = quotation.getGeneratedPdfFileName();
            if (fileName == null || fileName.isBlank()) fileName = "Quotation_" + quotation.getId() + ".pdf";
            String mediaId = uploadPdf(pdfBytes, fileName);
            sendDocument(to, mediaId, fileName, message);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to send quotation PDF through WhatsApp: " + e.getMessage(), e);
        }
    }

    private String uploadPdf(byte[] pdfBytes, String fileName) throws Exception {
        String boundary = "----IndoCrm" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, pdfBytes, fileName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.facebook.com/" + graphVersion + "/" + phoneNumberId + "/media"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) throw new IllegalStateException("WhatsApp media upload failed: " + response.body());
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
        if (!matcher.find()) throw new IllegalStateException("WhatsApp did not return a media id. Response: " + response.body());
        return matcher.group(1);
    }

    private void sendDocument(String to, String mediaId, String fileName, String message) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("messaging_product", "whatsapp");
        root.put("to", to);
        root.put("type", "document");
        ObjectNode document = root.putObject("document");
        document.put("id", mediaId);
        document.put("filename", fileName);
        if (message != null && !message.isBlank()) document.put("caption", message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.facebook.com/" + graphVersion + "/" + phoneNumberId + "/messages"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) throw new IllegalStateException("WhatsApp document send failed: " + response.body());
    }

    private byte[] multipartBody(String boundary, byte[] pdfBytes, String fileName) {
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"messaging_product\"\r\n\r\n"
                + "whatsapp\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"type\"\r\n\r\n"
                + "application/pdf\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n";
        String tail = "\r\n--" + boundary + "--\r\n";
        byte[] a = head.getBytes(StandardCharsets.UTF_8);
        byte[] c = tail.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[a.length + pdfBytes.length + c.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(pdfBytes, 0, result, a.length, pdfBytes.length);
        System.arraycopy(c, 0, result, a.length + pdfBytes.length, c.length);
        return result;
    }

    private String normalizeIndianOrInternationalPhone(String input) {
        String digits = input == null ? "" : input.replaceAll("\\D", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        if (digits.startsWith("0")) digits = digits.substring(1);
        if (digits.length() == 10) digits = "91" + digits;
        if (digits.length() < 10) throw new IllegalArgumentException("Enter a valid WhatsApp phone number including country code.");
        return digits;
    }
}