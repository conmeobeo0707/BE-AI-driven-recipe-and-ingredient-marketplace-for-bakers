package com.manhnv.capstonebakerai.recipe.video;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class VideoPhotoEmbedder {

    public static final int RENDER_BUDGET_CHARS = 6_000_000;
    private static final int SLIDE_BUDGET_CHARS = 850_000;
    private static final int MAX_DOWNLOAD_BYTES = 20 * 1024 * 1024;
    private static final int MAX_RAW_EMBED_BYTES = 350_000;
    private static final double OVERSCAN = 1.15;
    private static final float JPEG_QUALITY = 0.82f;

    private final RestClient client = RestClient.builder()
            .requestFactory(requestFactory())
            .build();

    public static final class Budget {
        private int remaining;

        public Budget(int chars) {
            this.remaining = chars;
        }
    }

    public List<String> embed(List<String> urls, int width, int height, Budget budget) {
        if (urls == null || urls.isEmpty()) return List.of();
        List<String> encoded = urls.parallelStream()
                .map(url -> toDataUri(url, width, height))
                .toList();
        List<String> result = new ArrayList<>();
        int slideChars = 0;
        synchronized (budget) {
            for (String uri : encoded) {
                if (uri == null) continue;
                if (slideChars + uri.length() > SLIDE_BUDGET_CHARS || uri.length() > budget.remaining) break;
                result.add(uri);
                slideChars += uri.length();
                budget.remaining -= uri.length();
            }
        }
        return result;
    }

    private String toDataUri(String url, int width, int height) {
        try {
            byte[] bytes = download(url);
            if (bytes == null) return null;
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) {
                if (isWebp(bytes) && bytes.length <= MAX_RAW_EMBED_BYTES) {
                    return "data:image/webp;base64," + Base64.getEncoder().encodeToString(bytes);
                }
                log.warn("Skipping video photo {}: unsupported or too large image", url);
                return null;
            }
            BufferedImage oriented = orient(source, exifOrientation(bytes));
            BufferedImage cropped = cover(oriented, (int) Math.round(width * OVERSCAN), (int) Math.round(height * OVERSCAN));
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg(cropped));
        } catch (Exception e) {
            log.warn("Skipping video photo {}: {}", url, e.getMessage());
            return null;
        }
    }

    private byte[] download(String url) {
        if (url == null || !url.startsWith("https://")) return null;
        byte[] bytes = client.get().uri(URI.create(url)).retrieve().body(byte[].class);
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_DOWNLOAD_BYTES) return null;
        return bytes;
    }

    private static BufferedImage cover(BufferedImage source, int targetWidth, int targetHeight) {
        double aspect = (double) targetWidth / targetHeight;
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int cropWidth = sourceWidth;
        int cropHeight = (int) Math.round(sourceWidth / aspect);
        if (cropHeight > sourceHeight) {
            cropHeight = sourceHeight;
            cropWidth = (int) Math.round(sourceHeight * aspect);
        }
        int cropX = (sourceWidth - cropWidth) / 2;
        int cropY = (sourceHeight - cropHeight) / 2;
        int outWidth = Math.min(targetWidth, cropWidth);
        int outHeight = Math.max(1, (int) Math.round(outWidth / aspect));

        BufferedImage out = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, outWidth, outHeight);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, outWidth, outHeight, cropX, cropY, cropX + cropWidth, cropY + cropHeight, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static byte[] jpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static BufferedImage orient(BufferedImage image, int orientation) {
        if (orientation <= 1 || orientation > 8) return image;
        int w = image.getWidth();
        int h = image.getHeight();
        boolean swap = orientation >= 5;
        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 2 -> { t.translate(w, 0); t.scale(-1, 1); }
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); }
            case 4 -> { t.translate(0, h); t.scale(1, -1); }
            case 5 -> { t.rotate(Math.PI / 2); t.scale(1, -1); }
            case 6 -> { t.translate(h, 0); t.rotate(Math.PI / 2); }
            case 7 -> { t.scale(-1, 1); t.translate(-h, 0); t.translate(0, w); t.rotate(-Math.PI / 2); }
            case 8 -> { t.translate(0, w); t.rotate(-Math.PI / 2); }
            default -> { }
        }
        BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, out.getWidth(), out.getHeight());
            g.drawImage(image, t, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    static int exifOrientation(byte[] jpeg) {
        if (jpeg.length < 4 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) return 1;
        int pos = 2;
        while (pos + 4 <= jpeg.length) {
            if ((jpeg[pos] & 0xFF) != 0xFF) return 1;
            int marker = jpeg[pos + 1] & 0xFF;
            if (marker == 0xDA || marker == 0xD9) return 1;
            int length = ((jpeg[pos + 2] & 0xFF) << 8) | (jpeg[pos + 3] & 0xFF);
            int segment = pos + 4;
            if (marker == 0xE1 && segment + 14 <= jpeg.length && length >= 16
                    && jpeg[segment] == 'E' && jpeg[segment + 1] == 'x' && jpeg[segment + 2] == 'i' && jpeg[segment + 3] == 'f') {
                return readTiffOrientation(jpeg, segment + 6, Math.min(jpeg.length, pos + 2 + length));
            }
            pos += 2 + length;
        }
        return 1;
    }

    private static int readTiffOrientation(byte[] b, int tiff, int end) {
        if (tiff + 8 > end) return 1;
        boolean little = b[tiff] == 'I' && b[tiff + 1] == 'I';
        if (!little && !(b[tiff] == 'M' && b[tiff + 1] == 'M')) return 1;
        int ifd = tiff + (int) readInt(b, tiff + 4, little);
        if (ifd + 2 > end) return 1;
        int count = readShort(b, ifd, little);
        for (int i = 0; i < count; i++) {
            int entry = ifd + 2 + i * 12;
            if (entry + 12 > end) return 1;
            if (readShort(b, entry, little) == 0x0112) {
                return readShort(b, entry + 8, little);
            }
        }
        return 1;
    }

    private static int readShort(byte[] b, int at, boolean little) {
        return little ? (b[at] & 0xFF) | ((b[at + 1] & 0xFF) << 8)
                : ((b[at] & 0xFF) << 8) | (b[at + 1] & 0xFF);
    }

    private static long readInt(byte[] b, int at, boolean little) {
        return little
                ? (b[at] & 0xFFL) | ((b[at + 1] & 0xFFL) << 8) | ((b[at + 2] & 0xFFL) << 16) | ((b[at + 3] & 0xFFL) << 24)
                : ((b[at] & 0xFFL) << 24) | ((b[at + 1] & 0xFFL) << 16) | ((b[at + 2] & 0xFFL) << 8) | (b[at + 3] & 0xFFL);
    }

    private static boolean isWebp(byte[] b) {
        return b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        return factory;
    }
}
