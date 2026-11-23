
package com.indocrm.indocrm.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.indocrm.indocrm.model.QuotationTemplate.DesignBlock;

/**
 * Letterhead used by every built-in quotation PDF:
 *   - letterpad : full-page A4 background.
 *   - logo      : uploaded by the Super Admin.
 *   - footer    : phone / website / address.
 */
@Service
public class QuotationBrandingService {

    private static final double PW = 595.28, PH = 841.89;
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    static final String DEF_PHONE = "8925811993 / 8925811994";
    static final String DEF_WEB = "www.indocampas.com";
    static final String DEF_ADDRESS =
            "TIDEL NEO - Thiruchitrambalam\nKoot Road, Tamil Nadu 605111";

    @Value("${quotation.storage.dir:./storage/quotations}")
    private String storageDirProp;

    // ───────────────────────── storage ─────────────────────────

    private Path dir() throws IOException {
        Path p = Paths.get(storageDirProp)
                .toAbsolutePath()
                .normalize()
                .resolve("branding");

        Files.createDirectories(p);
        return p;
    }

    private Path logoFile() throws IOException {
        return dir().resolve("logo.png");
    }

    private Path letterpadFile() throws IOException {
        return dir().resolve("letterpad.jpg");
    }

    private Path settingsFile() throws IOException {
        return dir().resolve("branding.properties");
    }

    private Properties settings() {
        Properties p = new Properties();

        try {
            Path f = settingsFile();

            if (Files.isRegularFile(f)) {
                try (InputStream in = Files.newInputStream(f)) {
                    p.load(new java.io.InputStreamReader(in, "UTF-8"));
                }
            }
        } catch (Exception ignored) {
        }

        return p;
    }

    // ───────────────────────── public API ─────────────────────────

    public synchronized Map<String, Object> save(
            String type, MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please choose an image file.");
        }

        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "Image must be smaller than 10 MB.");
        }

        BufferedImage img = ImageIO.read(
                new ByteArrayInputStream(file.getBytes()));

        if (img == null) {
            throw new IllegalArgumentException(
                    "Unsupported image. Please upload a PNG or JPG file.");
        }

        if ("logo".equalsIgnoreCase(type)) {

            BufferedImage out = scaleDown(
                    img,
                    1400,
                    700,
                    BufferedImage.TYPE_INT_ARGB,
                    null
            );

            ImageIO.write(out, "png", logoFile().toFile());

        } else if ("letterpad".equalsIgnoreCase(type)) {

            double ratio = img.getHeight() / (double) img.getWidth();

            if (ratio < 1.30 || ratio > 1.52) {
                throw new IllegalArgumentException(
                        "Letterpad must be an A4 portrait image "
                        + "(for example 2480 x 3508 px). "
                        + "The uploaded image is "
                        + img.getWidth() + " x " + img.getHeight() + ".");
            }

            BufferedImage out = scaleDown(
                    img,
                    1654,
                    2339,
                    BufferedImage.TYPE_INT_RGB,
                    Color.WHITE
            );

            writeJpeg(out, letterpadFile(), 0.9f);

        } else {
            throw new IllegalArgumentException(
                    "Unknown branding type: " + type);
        }

        return status();
    }

    public synchronized Map<String, Object> delete(
            String type) throws IOException {

        if ("logo".equalsIgnoreCase(type)) {
            Files.deleteIfExists(logoFile());

        } else if ("letterpad".equalsIgnoreCase(type)) {
            Files.deleteIfExists(letterpadFile());

        } else {
            throw new IllegalArgumentException(
                    "Unknown branding type: " + type);
        }

        return status();
    }

    public synchronized Map<String, Object> saveSettings(
            Map<String, Object> in) throws IOException {

        Properties p = settings();

        if (in.containsKey("showBrandText")) {
            p.setProperty(
                    "showBrandText",
                    String.valueOf(
                            Boolean.parseBoolean(
                                    String.valueOf(in.get("showBrandText"))
                            )
                    )
            );
        }

        for (String k : new String[]{"phone", "website", "address"}) {
            if (in.containsKey(k)) {
                p.setProperty(
                        k,
                        String.valueOf(
                                in.get(k) == null ? "" : in.get(k)
                        ).trim()
                );
            }
        }

        try (java.io.Writer w =
                new java.io.OutputStreamWriter(
                        Files.newOutputStream(settingsFile()), "UTF-8")) {

            p.store(w, "quotation branding");
        }

        return status();
    }

    public Map<String, Object> status() throws IOException {

        Properties p = settings();

        Map<String, Object> m = new LinkedHashMap<>();

        boolean hasLogo = Files.isRegularFile(logoFile());
        boolean hasPad = Files.isRegularFile(letterpadFile());

        m.put("hasLogo", hasLogo);
        m.put("hasLetterpad", hasPad);
        m.put("usingDefaultLetterpad", !hasPad);

        m.put(
                "showBrandText",
                Boolean.parseBoolean(
                        p.getProperty("showBrandText", "false")
                )
        );

        m.put("phone", p.getProperty("phone", DEF_PHONE));
        m.put("website", p.getProperty("website", DEF_WEB));
        m.put("address", p.getProperty("address", DEF_ADDRESS));

        m.put(
                "logoDataUrl",
                hasLogo
                        ? dataUrl("image/png", Files.readAllBytes(logoFile()))
                        : ""
        );

        byte[] pad = letterpadBytes();

        m.put(
                "letterpadDataUrl",
                pad == null
                        ? ""
                        : dataUrl("image/jpeg", thumbnailJpeg(pad, 520))
        );

        return m;
    }

    /**
     * Logo (PNG) uploaded by the Super Admin,
     * or null when none was uploaded.
     */
    public byte[] logoBytes() {
        try {
            return Files.isRegularFile(logoFile())
                    ? Files.readAllBytes(logoFile())
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Footer text + brand-text switch
     * (phone, website, address, showBrandText)
     * with defaults applied.
     */
    public Map<String, Object> footerSettings() {

        Properties p = settings();

        Map<String, Object> m = new LinkedHashMap<>();

        m.put(
                "showBrandText",
                Boolean.parseBoolean(
                        p.getProperty("showBrandText", "false")
                )
        );

        m.put("phone", p.getProperty("phone", DEF_PHONE));
        m.put("website", p.getProperty("website", DEF_WEB));
        m.put("address", p.getProperty("address", DEF_ADDRESS));

        return m;
    }

    /** All blocks of the letterhead (background, logo, brand text, footer). */
    public List<DesignBlock> buildLetterhead() {

        List<DesignBlock> b = new ArrayList<>();

        try {
            Properties s = settings();

            byte[] pad = letterpadBytes();

            if (pad != null) {
                b.add(image(
                        "letterpad",
                        0, 0, 1, 1,
                        dataUrl("image/jpeg", pad)
                ));
            }

            double textX = 0.094;

            if (Files.isRegularFile(logoFile())) {

                byte[] logoBytes = Files.readAllBytes(logoFile());

                BufferedImage li = ImageIO.read(
                        new ByteArrayInputStream(logoBytes));

                if (li != null) {

                    double boxW = 0.46 * PW;
                    double boxH = 0.105 * PH;

                    double scale = Math.min(
                            boxW / li.getWidth(),
                            boxH / li.getHeight()
                    );

                    double fw = li.getWidth() * scale / PW;
                    double fh = li.getHeight() * scale / PH;

                    b.add(image(
                            "logo",
                            0.094,
                            0.028 + (0.105 - fh) / 2,
                            fw,
                            fh,
                            dataUrl("image/png", logoBytes)
                    ));

                    textX = 0.094 + fw + 0.02;
                }
            }

            if (Boolean.parseBoolean(
                    s.getProperty("showBrandText", "false"))) {

                double w = Math.max(0.2, 0.84 - textX);

                b.add(text(
                        "brandName",
                        textX, 0.045, w, 0.05,
                        "CAMPAS TECH",
                        24, "Helvetica", false, "#7B1113"
                ));

                b.add(text(
                        "brandSub",
                        textX, 0.098, w, 0.035,
                        "Private Limited",
                        14, "Times", false, "#7B1113"
                ));
            }

            // Footer — round icons + text

            String phone = s.getProperty("phone", DEF_PHONE);
            String web = s.getProperty("website", DEF_WEB);
            String address = s.getProperty("address", DEF_ADDRESS);

            double iw = 19 / PW;
            double ih = 19 / PH;
            double ix = 0.14;
            double tx = 0.185;
            double tw = 0.62;

            b.add(image(
                    "phoneIcon",
                    ix, 0.842, iw, ih,
                    QuotationIcons.dataUrl("phone")
            ));

            b.add(text(
                    "phoneText",
                    tx, 0.838, tw, 0.028,
                    phone,
                    10, "Helvetica", false, "#111827"
            ));

            b.add(image(
                    "globeIcon",
                    ix, 0.880, iw, ih,
                    QuotationIcons.dataUrl("globe")
            ));

            b.add(text(
                    "webText",
                    tx, 0.876, tw, 0.028,
                    web,
                    10, "Helvetica", false, "#1D4ED8"
            ));

            b.add(image(
                    "pinIcon",
                    ix, 0.914, iw, ih,
                    QuotationIcons.dataUrl("pin")
            ));

            DesignBlock addr = text(
                    "addrText",
                    tx, 0.910, tw, 0.05,
                    address,
                    10, "Helvetica", false, "#C8102E"
            );

            addr.setVerticalAlign("top");
            b.add(addr);

        } catch (Exception e) {
            // Never block a quotation because of a letterhead problem.
        }

        return b;
    }

    // ───────────────────────── helpers ─────────────────────────

    /**
     * Uploaded letterpad, else classpath letterpad-default.jpg.
     */
    public byte[] letterpadBytes() {

        try {
            if (Files.isRegularFile(letterpadFile())) {
                return Files.readAllBytes(letterpadFile());
            }
        } catch (Exception ignored) {
        }

        try (InputStream in = getClass().getResourceAsStream(
                "/quotation-templates/letterpad-default.jpg")) {

            return in == null ? null : in.readAllBytes();

        } catch (Exception e) {
            return null;
        }
    }

    private static String dataUrl(String mime, byte[] bytes) {
        return "data:" + mime + ";base64,"
                + Base64.getEncoder().encodeToString(bytes);
    }

    private static BufferedImage scaleDown(
            BufferedImage src,
            int maxW,
            int maxH,
            int type,
            Color fill) {

        double f = Math.min(
                1.0,
                Math.min(
                        maxW / (double) src.getWidth(),
                        maxH / (double) src.getHeight()
                )
        );

        int w = Math.max(1, (int) Math.round(src.getWidth() * f));
        int h = Math.max(1, (int) Math.round(src.getHeight() * f));

        BufferedImage out = new BufferedImage(w, h, type);

        Graphics2D g = out.createGraphics();

        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        g.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        if (fill != null) {
            g.setColor(fill);
            g.fillRect(0, 0, w, h);
        }

        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();

        return out;
    }

    private static void writeJpeg(
            BufferedImage img,
            Path file,
            float quality) throws IOException {

        ImageWriter writer =
                ImageIO.getImageWritersByFormatName("jpeg").next();

        ImageWriteParam param = writer.getDefaultWriteParam();

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (
                OutputStream os = Files.newOutputStream(file);
                ImageOutputStream ios =
                        ImageIO.createImageOutputStream(os)
        ) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);

        } finally {
            writer.dispose();
        }
    }

    private static byte[] thumbnailJpeg(byte[] src, int maxW) {

        try {
            BufferedImage im = ImageIO.read(
                    new ByteArrayInputStream(src));

            if (im == null) {
                return src;
            }

            BufferedImage t = scaleDown(
                    im,
                    maxW,
                    maxW * 2,
                    BufferedImage.TYPE_INT_RGB,
                    Color.WHITE
            );

            ByteArrayOutputStream bo = new ByteArrayOutputStream();

            ImageIO.write(t, "jpg", bo);

            return bo.toByteArray();

        } catch (Exception e) {
            return src;
        }
    }

    private DesignBlock image(
            String id,
            double x,
            double y,
            double w,
            double h,
            String dataUrl) {

        DesignBlock b = new DesignBlock();

        b.setId(id);
        b.setType("image");

        b.setX(x);
        b.setY(y);
        b.setWidth(w);
        b.setHeight(h);

        b.setImageDataUrl(dataUrl);

        return b;
    }

    private DesignBlock text(
            String id,
            double x,
            double y,
            double w,
            double h,
            String text,
            double size,
            String family,
            boolean bold,
            String color) {

        DesignBlock d = new DesignBlock();

        d.setId(id);
        d.setType("text");

        d.setX(x);
        d.setY(y);
        d.setWidth(w);
        d.setHeight(h);

        d.setText(text);
        d.setMapsTo("");

        d.setFontSize(size);
        d.setFontFamily(family);
        d.setBold(bold);
        d.setColor(color);

        d.setBackgroundColor("transparent");
        d.setAlign("left");
        d.setVerticalAlign("middle");
        d.setPadding(1);

        return d;
    }
}
