package com.manhnv.capstonebakerai.recipe.suggestion.helper;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.suggestion.exception.IngredientImageErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

@Component
public class IngredientImagePreprocessor {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private static final long MAX_PIXELS = 16_000_000L;

    private static final int MAX_INPUT_SIDE = 10_000;

    private static final int MAX_OUTPUT_SIDE = 1536;

    private static final Set<String> ALLOWED_FORMATS =
            Set.of("jpeg", "jpg", "png");

    public String toDataUrl(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(
                    IngredientImageErrorCode.IMAGE_REQUIRED
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(
                    IngredientImageErrorCode.IMAGE_TOO_LARGE
            );
        }

        try (
                var input = new MemoryCacheImageInputStream(
                        new ByteArrayInputStream(file.getBytes())
                )
        ) {
            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw new AppException(
                        IngredientImageErrorCode.UNSUPPORTED_IMAGE_TYPE
                );
            }

            ImageReader reader = readers.next();

            try {
                String format = reader.getFormatName()
                        .toLowerCase(Locale.ROOT);

                if (!ALLOWED_FORMATS.contains(format)) {
                    throw new AppException(
                            IngredientImageErrorCode.UNSUPPORTED_IMAGE_TYPE
                    );
                }

                reader.setInput(input, true, true);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0) {
                    throw new AppException(
                            IngredientImageErrorCode.INVALID_IMAGE
                    );
                }

                if (width > MAX_INPUT_SIDE
                        || height > MAX_INPUT_SIDE
                        || (long) width * height > MAX_PIXELS) {
                    throw new AppException(
                            IngredientImageErrorCode.IMAGE_DIMENSIONS_TOO_LARGE
                    );
                }

                BufferedImage original = reader.read(0);

                if (original == null) {
                    throw new AppException(
                            IngredientImageErrorCode.INVALID_IMAGE
                    );
                }

                double scale = Math.min(
                        1.0,
                        MAX_OUTPUT_SIDE / (double) Math.max(width, height)
                );

                int outputWidth = Math.max(
                        1,
                        (int) Math.round(width * scale)
                );

                int outputHeight = Math.max(
                        1,
                        (int) Math.round(height * scale)
                );

                BufferedImage resized = new BufferedImage(
                        outputWidth,
                        outputHeight,
                        BufferedImage.TYPE_INT_RGB
                );

                Graphics2D graphics = resized.createGraphics();

                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(
                            0, 0, outputWidth, outputHeight
                    );

                    graphics.setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR
                    );

                    graphics.drawImage(
                            original,
                            0, 0,
                            outputWidth, outputHeight,
                            null
                    );
                } finally {
                    graphics.dispose();
                }

                ByteArrayOutputStream output =
                        new ByteArrayOutputStream();

                if (!ImageIO.write(resized, "jpeg", output)) {
                    throw new AppException(
                            IngredientImageErrorCode.INVALID_IMAGE
                    );
                }

                return "data:image/jpeg;base64,"
                        + Base64.getEncoder()
                        .encodeToString(output.toByteArray());

            } finally {
                reader.dispose();
            }

        } catch (IOException | IllegalArgumentException exception) {
            throw new AppException(
                    IngredientImageErrorCode.INVALID_IMAGE
            );
        }
    }
}