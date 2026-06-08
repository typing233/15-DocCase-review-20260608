package com.doccase.ocr.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
@Component
public class GrayscaleProcessor implements ImagePreprocessor {

    @Override
    public byte[] process(byte[] image) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(image));
            if (original == null) {
                log.warn("Could not read image for grayscale processing, returning original");
                return image;
            }

            BufferedImage grayscale = new BufferedImage(
                    original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = grayscale.createGraphics();
            g.drawImage(original, 0, 0, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(grayscale, "png", baos);

            log.debug("Grayscale processing completed");
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Grayscale processing failed, returning original image", e);
            return image;
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
