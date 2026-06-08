package com.doccase.ocr.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
@Component
public class DenoiseProcessor implements ImagePreprocessor {

    @Override
    public byte[] process(byte[] image) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(image));
            if (original == null) {
                log.warn("Could not read image for denoise processing, returning original");
                return image;
            }

            int width = original.getWidth();
            int height = original.getHeight();
            BufferedImage denoised = new BufferedImage(width, height, original.getType());

            // Simple median filter for noise reduction (3x3 kernel)
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    int[] neighbors = new int[9];
                    int idx = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            neighbors[idx++] = original.getRGB(x + dx, y + dy) & 0xFF;
                        }
                    }
                    java.util.Arrays.sort(neighbors);
                    int median = neighbors[4];
                    int rgb = (median << 16) | (median << 8) | median;
                    denoised.setRGB(x, y, rgb);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(denoised, "png", baos);

            log.debug("Denoise processing completed");
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Denoise processing failed, returning original image", e);
            return image;
        }
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
