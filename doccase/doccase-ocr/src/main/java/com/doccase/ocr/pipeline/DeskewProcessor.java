package com.doccase.ocr.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
@Component
public class DeskewProcessor implements ImagePreprocessor {

    @Override
    public byte[] process(byte[] image) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(image));
            if (original == null) {
                log.warn("Could not read image for deskew processing, returning original");
                return image;
            }

            // Detect skew angle using projection profile method
            double angle = detectSkewAngle(original);

            if (Math.abs(angle) < 0.5) {
                // Skip rotation if angle is negligible
                log.debug("Skew angle {} is negligible, skipping deskew", angle);
                return image;
            }

            // Rotate image to correct skew
            BufferedImage deskewed = rotateImage(original, -angle);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(deskewed, "png", baos);

            log.debug("Deskew processing completed, corrected angle: {}", angle);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Deskew processing failed, returning original image", e);
            return image;
        }
    }

    @Override
    public int getOrder() {
        return 3;
    }

    private double detectSkewAngle(BufferedImage image) {
        // Simplified skew detection using horizontal projection profile
        // Checks angles from -5 to +5 degrees to find the one with maximum variance
        int width = image.getWidth();
        int height = image.getHeight();

        double bestAngle = 0;
        double maxVariance = 0;

        for (double angle = -5.0; angle <= 5.0; angle += 0.5) {
            double variance = calculateProjectionVariance(image, angle, width, height);
            if (variance > maxVariance) {
                maxVariance = variance;
                bestAngle = angle;
            }
        }

        return bestAngle;
    }

    private double calculateProjectionVariance(BufferedImage image, double angle, int width, int height) {
        double radians = Math.toRadians(angle);
        int[] projection = new int[height];

        int sampleStep = Math.max(1, width / 100);
        for (int y = 0; y < height; y++) {
            int count = 0;
            for (int x = 0; x < width; x += sampleStep) {
                int rotX = (int) (x * Math.cos(radians) - y * Math.sin(radians));
                int rotY = (int) (x * Math.sin(radians) + y * Math.cos(radians));
                if (rotX >= 0 && rotX < width && rotY >= 0 && rotY < height) {
                    int pixel = image.getRGB(rotX, rotY) & 0xFF;
                    if (pixel < 128) {
                        count++;
                    }
                }
            }
            projection[y] = count;
        }

        // Calculate variance
        double mean = 0;
        for (int p : projection) mean += p;
        mean /= projection.length;

        double variance = 0;
        for (int p : projection) variance += (p - mean) * (p - mean);
        return variance / projection.length;
    }

    private BufferedImage rotateImage(BufferedImage image, double angle) {
        double radians = Math.toRadians(angle);
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage rotated = new BufferedImage(width, height, image.getType());
        Graphics2D g = rotated.createGraphics();
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, width, height);

        AffineTransform at = new AffineTransform();
        at.rotate(radians, width / 2.0, height / 2.0);
        g.setTransform(at);
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return rotated;
    }
}
