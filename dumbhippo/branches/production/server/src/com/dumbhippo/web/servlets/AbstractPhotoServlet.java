package com.dumbhippo.web.servlets;

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
import com.dumbhippo.ImageUtils;
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
	
	protected Collection<BufferedImage> readScaledPhotos(FileItem photo) throws IOException, HumanVisibleException {
		BufferedImage image = readPhoto(photo);
		
		Collection<BufferedImage> images = new ArrayList<BufferedImage>(PHOTO_DIMENSIONS.length);
		
		for (int size : PHOTO_DIMENSIONS) {
			images.add(ImageUtils.scaleTo(image, size));
		}
		
		return images;
	}

	protected BufferedImage readScaledPhoto(FileItem photo, int xSize, int ySize) throws IOException, HumanVisibleException {
		BufferedImage image = readPhoto(photo);
		return ImageUtils.scaleTo(image, xSize, ySize);
	}
	
	protected void doFinalRedirect(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, HumanVisibleException {
		if (reloadTo == null)
			reloadTo = "/";
		response.sendRedirect(reloadTo);
	}
}
