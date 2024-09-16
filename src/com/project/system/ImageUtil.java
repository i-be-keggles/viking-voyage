package com.project.system;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.nio.Buffer;
import java.util.*;

public class ImageUtil {

    //returns image rotated angle degrees scale big
    public static BufferedImage rotate(BufferedImage image, float angle, float scale){
        //fix weird scaling issues
        final float fixScale = 0.43f;
        float fixStrength = Math.abs((angle % 90f) - ((angle % 90f > 45)? 90f : 0f));
        fixStrength = 1 + (fixStrength / 45 * fixScale);
        if(fixStrength <= 0 || Float.isInfinite(fixStrength)) fixStrength = 1;
        fixStrength += fixStrength * scale;

        //handle rotation
        final double rads = Math.toRadians(angle);
        final double sin = Math.abs(Math.sin(rads));
        final double cos = Math.abs(Math.cos(rads));
        final int w = (int) (Math.floor(image.getWidth() * cos + image.getHeight() * sin) / fixStrength);
        final int h = (int) (Math.floor(image.getHeight() * cos + image.getWidth() * sin) / fixStrength);
        final BufferedImage rotatedImage = new BufferedImage(w, h, Transparency.TRANSLUCENT);
        final AffineTransform at = new AffineTransform();
        at.translate(w / 2f, h / 2f);
        at.rotate(rads,0, 0);
        at.translate(-image.getWidth() / 2f, -image.getHeight() / 2f);

        //finalize and return image
        final AffineTransformOp rotateOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        rotateOp.filter(image,rotatedImage);
        return rotatedImage;
    }

    //returns image with size size with pixels pixels as big as they used to be
    public static BufferedImage pixelize(BufferedImage image, int pixels, Point size) {
        BufferedImage scaled = new BufferedImage(size.x, size.y, Transparency.TRANSLUCENT);

        //scale down
        AffineTransform at = new AffineTransform();
        at.scale(1/(float)pixels, 1/(float)pixels);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        scaled = scaleOp.filter(image, scaled);

        //save
        image = scaled;
        scaled = new BufferedImage(size.x, size.y, Transparency.TRANSLUCENT);

        //scale back
        at = new AffineTransform();
        at.scale(pixels, pixels);
        scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        scaled = scaleOp.filter(image, scaled);

        //return image
        return scaled;
    }
}
