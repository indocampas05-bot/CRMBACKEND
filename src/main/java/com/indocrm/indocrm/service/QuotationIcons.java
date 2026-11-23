package com.indocrm.indocrm.service;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/** Draws the three small round footer icons (phone / website / location) of the IndoCampas letterhead. */
final class QuotationIcons {
    private QuotationIcons() {}

    private static final int S = 160;

    static byte[] png(String kind) {
        BufferedImage im = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Color red = new Color(0xC8102E), navy = new Color(0x0F1A2E);
        Color bg = "globe".equals(kind) ? navy : red;
        g.setColor(bg);
        g.fill(new Ellipse2D.Double(3, 3, S - 6, S - 6));
        g.setColor(Color.WHITE);
        switch (kind) {
            case "phone": phone(g, bg); break;
            case "globe": globe(g); break;
            default: pin(g, bg); break;
        }
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(im, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static void phone(Graphics2D g, Color bg) {
        // handset: quarter-circle bar bulging to the bottom-left with a rounded pad at each end
        double cx = 98, cy = 62, r = 50;
        g.setStroke(new BasicStroke(17f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 172, 106, Arc2D.OPEN));
        for (double ang : new double[]{172, 278}) {
            double x = cx + r * Math.cos(Math.toRadians(ang));
            double y = cy - r * Math.sin(Math.toRadians(ang));
            AffineTransform old = g.getTransform();
            g.rotate(Math.toRadians(-(ang - 90)), x, y);
            g.fill(new RoundRectangle2D.Double(x - 15, y - 12, 30, 24, 10, 10));
            g.setTransform(old);
        }
    }

    private static void globe(Graphics2D g) {
        double c = S / 2.0, r = 40;
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Double(c - r, c - r, 2 * r, 2 * r));
        g.draw(new Ellipse2D.Double(c - r * 0.45, c - r, r * 0.9, 2 * r));
        g.draw(new Line2D.Double(c - r, c, c + r, c));
        g.draw(new Arc2D.Double(c - r, c - r * 0.55 - r * 0.45, 2 * r, r * 0.9, 200, 140, Arc2D.OPEN));
        g.draw(new Arc2D.Double(c - r, c + r * 0.55 - r * 0.45, 2 * r, r * 0.9, 20, 140, Arc2D.OPEN));
    }

    private static void pin(Graphics2D g, Color bg) {
        double c = S / 2.0;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(c, 124);
        p.curveTo(c - 6, 106, c - 38, 82, c - 38, 62);
        p.curveTo(c - 38, 40, c - 22, 28, c, 28);
        p.curveTo(c + 22, 28, c + 38, 40, c + 38, 62);
        p.curveTo(c + 38, 82, c + 6, 106, c, 124);
        p.closePath();
        g.fill(p);
        g.setColor(bg);
        g.fill(new Ellipse2D.Double(c - 14, 48, 28, 28));
    }

    static String dataUrl(String kind) {
        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(png(kind));
    }

    public static void main(String[] a) throws Exception {
        for (String k : new String[]{"phone", "globe", "pin"})
            java.nio.file.Files.write(java.nio.file.Path.of("/tmp/jt/" + k + ".png"), png(k));
    }
}