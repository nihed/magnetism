package com.dumbhippo.web;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.server.HumanVisibleException;

public abstract class AbstractPhotoServlet extends AbstractSmallImageServlet {
	private static final int PHOTO_DIMENSION = 48;
	
	private static final Log logger = GlobalSetup.getLog(AbstractPhotoServlet.class);

	@Override
	protected String getDefaultFilename() {
		return "default";
	}	
	
	protected BufferedImage readScaledPhoto(FileItem photo) throws IOException, HumanVisibleException {
		BufferedImage image = readPhoto(photo);
		// FIXME It would be nicer to only pad images so they are always square and
		// our
		// css won't mangle their aspect ratio. I think to do this we have to
		// manually
		// create the destination BufferedImage and then transform onto it.
		
		// FIXME it would be nicer to avoid round-tripping a jpeg through a
		// BufferedImage

		double scaleX, scaleY;
		if (image.getHeight() > image.getWidth()) {
			scaleY = PHOTO_DIMENSION / (double) image.getHeight();
			scaleX = scaleY;
		} else {
			scaleX = PHOTO_DIMENSION / (double) image.getWidth();
			scaleY = scaleX;
		}

		logger.debug("Scaling photo scaleX = " + scaleX + " scaleY = " + scaleY + "new width = "
				+ image.getWidth() * scaleX + " new height = " + image.getHeight() * scaleY);

		AffineTransform tx = new AffineTransform();
		tx.scale(scaleX, scaleY);
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
		return op.filter(image, null);		
	}

	protected void doFinalRedirect(HttpServletRequest request, HttpServletResponse response, String filename, int version, String title, String url) throws ServletException, IOException, HumanVisibleException {
		request.setAttribute("photoFilename", filename);		
		request.setAttribute("photoVersion", version);		
		XmlBuilder link = new XmlBuilder();
		link.appendTextNode("a", title, "href", url);
		request.setAttribute("homePageLink", link.toString());
		request.getRequestDispatcher("/newphoto").forward(request, response);			
	}
}
