package com.dumbhippo.web.servlets;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HumanVisibleException;

public abstract class AbstractPhotoServlet extends AbstractSmallImageServlet {
	private static final int DEFAULT_SIZE = Configuration.SHOT_MEDIUM_SIZE;
	private static final int[] PHOTO_DIMENSIONS = { Configuration.SHOT_LARGE_SIZE, Configuration.SHOT_MEDIUM_SIZE, Configuration.SHOT_SMALL_SIZE, Configuration.SHOT_TINY_SIZE };
	protected static final int LARGEST_PHOTO_DIMENSION = Configuration.SHOT_LARGE_SIZE;
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(AbstractPhotoServlet.class);
	
	@Override
	protected String getDefaultFilename() {
		return "default";
	}
	
	@Override
	protected boolean needsSize() {
		return true;
	}

	@Override
	protected String getDefaultSize() {
		return Integer.toString(DEFAULT_SIZE);
	}
	
	private BufferedImage scaleTo(BufferedImage image, int xSize, int ySize) {
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
	
	private BufferedImage scaleTo(BufferedImage image, int size) {
		return scaleTo(image, size, size);
	}
	
	protected Collection<BufferedImage> readScaledPhotos(FileItem photo) throws IOException, HumanVisibleException {
		BufferedImage image = readPhoto(photo);
		
		Collection<BufferedImage> images = new ArrayList<BufferedImage>(PHOTO_DIMENSIONS.length);
		
		for (int size : PHOTO_DIMENSIONS) {
			images.add(scaleTo(image, size));
		}
		
		return images;
	}

	protected BufferedImage readScaledPhoto(FileItem photo, int xSize, int ySize) throws IOException, HumanVisibleException {
		BufferedImage image = readPhoto(photo);
		return scaleTo(image, xSize, ySize);
	}
	
	protected void doFinalRedirect(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, HumanVisibleException {
		if (reloadTo == null)
			reloadTo = "/";
		response.sendRedirect(reloadTo);
	}
}
