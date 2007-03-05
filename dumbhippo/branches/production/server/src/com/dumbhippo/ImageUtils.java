package com.dumbhippo;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class ImageUtils {
	public static BufferedImage scaleTo(BufferedImage image, int xSize, int ySize) {
		/* To avoid questions of aspect ratio on the client side, we always 
		 * generate a square image, adding transparent padding if required.
		 * If it happens that image was originally a JPEG exactly size x size,
		 * then it would be better to use that without converting to a PNG.
		 * We don't expect that to happen very often.
		 */
		int origWidth = image.getWidth();
		int origHeight = image.getHeight();

		double xScale = (double)xSize / origWidth;
		double yScale = (double)ySize / origHeight;
		
		double scale = Math.min(xScale, yScale);
		
		int newWidth = (int)Math.round(scale * origWidth);
		int newHeight = (int)Math.round(scale * origHeight);
		int translateX = (xSize - newWidth) / 2;
		int translateY = (ySize - newHeight) / 2;
		
		/* When upscaling, or scaling down by a small amount, we use Java2D and 
		 * bi-cubic filtering. When scaling down by a larger factor, even using  
		 * bi-cubic filtering gives pretty bad results, since we are sampling only 
		 * a small number of source pixels. So, instead, we use the old java.awt image 
		 * scaling facility which has SCALE_AREA_AVERAGING, which averages all pixels. 
		 * Performance is not significant for us.
		 */
		Image scaled;
		
		if (scale > 0.75 && scale != 1.0) {
			AffineTransform tx = AffineTransform.getScaleInstance(scale, scale);
			AffineTransformOp transformOp = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
			
			scaled = transformOp.filter(image, null);
		} else {
			scaled = image.getScaledInstance(newWidth, newHeight, Image.SCALE_AREA_AVERAGING);
		}
		
		/* If the source is both square and doesn't have alpha, we could use
		 * TYPE_INT_RGB instead. */
		BufferedImage dest = new BufferedImage(xSize, ySize, BufferedImage.TYPE_INT_ARGB_PRE);

		Graphics2D graphics = dest.createGraphics();
		graphics.drawImage(scaled, translateX, translateY, null);
		
		return dest;	
	}
	
	public static BufferedImage scaleTo(BufferedImage image, int size) {
		return scaleTo(image, size, size);
	}
}