package com.doccase.ocr.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
@Component
public class BinarizeProcessor implements ImagePreprocessor {

    @Override
    public byte[] process(byte[] image) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(image));
            if (original == null) {
                log.warn("Could not read image for binarize processing, returning original");
                return image;
            }

            int width = original.getWidth();
            int height = original.getHeight();
            BufferedImage binarized = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

            // Otsu's threshold method
            int threshold = calculateOtsuThreshold(original);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = original.getRGB(x, y) & 0xFF;
                    int bw = pixel < threshold ? 0x000000 : 0xFFFFFF;
                    binarized.setRGB(x, y, bw);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(binarized, "png", baos);

            log.debug("Binarize processing completed with threshold: {}", threshold);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Binarize processing failed, returning original image", e);
            return image;
        }
    }

    @Override
    public int getOrder() {
        return 4;
    }

    private int calculateOtsuThreshold(BufferedImage image) {
        int[] histogram = new int[256];
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y) & 0xFF;
                histogram[pixel]++;
            }
        }

        double sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }

        double sumB = 0;
        int wB = 0;
        double maxVariance = 0;
        int threshold = 0;

        for (int t = 0; t < 256; t++) {
            wB += histogram[t];
            if (wB == 0) continue;

            int wF = totalPixels - wB;
            if (wF == 0) break;

            sumB += t * histogram[t];
            double mB = sumB / wB;
            double mF = (sum - sumB) / wF;
            double variance = (double) wB * wF * (mB - mF) * (mB - mF);

            if (variance > maxVariance) {
                maxVariance = variance;
                threshold = t;
            }
        }

        return threshold;
    }
}
