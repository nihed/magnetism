package com.dumbhippo.web.servlets;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.web.WebEJBUtil;

public abstract class AbstractSmallImageServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = GlobalSetup.getLogger(AbstractSmallImageServlet.class);

	private static final int MAX_FILE_SIZE = 1024 * 1024 * 5; // 5M is huge, but photos can be big...
	// scaling something huge is probably bad, but allowing a typical desktop background is 
	// good if we can handle it...
	private static final int MAX_IMAGE_DIMENSION = 2048;
	
	protected Configuration config;
	protected IdentitySpider identitySpider;
	protected File saveDir;
	protected String reloadTo;
	
	protected abstract String getRelativePath();
	
	protected abstract String getDefaultFilename();
	
	protected abstract boolean needsSize();
	protected abstract String getDefaultSize();
	
	@Override
	public void init() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		
		String filesUrl = config.getPropertyFatalIfUnset(HippoProperty.FILES_SAVEURL);
		String saveUrl = filesUrl + getRelativePath();
		
		URI saveUri;
		try {
			saveUri = new URI(saveUrl);
		} catch (URISyntaxException e) {
			throw new RuntimeException("save url busted", e);
		}
		saveDir = new File(saveUri);
	}	
	
	protected BufferedImage readPhoto(FileItem photo) throws IOException, HumanVisibleException {
		InputStream in = photo.getInputStream();

		BufferedImage image = ImageIO.read(in);
		if (image == null) {
			throw new HumanVisibleException("Our computer can't load that photo; either it's too dumb, or possibly you uploaded a file that isn't a picture?");
		}
		if (image.getWidth() > MAX_IMAGE_DIMENSION || image.getHeight() > MAX_IMAGE_DIMENSION) {
			throw new HumanVisibleException("That photo is really huge, which blows our computer's mind. Can you send us a smaller one?");
		}
		return image;
	}
	
	protected abstract void doUpload(HttpServletRequest request, HttpServletResponse response, User user, Map<String,String> params, FileItem photo)
	throws HttpException, IOException, ServletException, HumanVisibleException;
	
	
	protected abstract boolean getSaveJpegAlso();
	
	private void doSave(BufferedImage scaled, String fileName, boolean overwrite, String format)
		throws HumanVisibleException {
		File saveDest = new File(saveDir, fileName);

		// ImageIO.write returns false if it doesn't understand the format 
		// name and throws IOException on any other error
		try {
			if (!overwrite && saveDest.exists())
				throw new IOException("Can't overwrite existing file");

			if (logger.isDebugEnabled())
				logger.debug("saving to {}", saveDest.getCanonicalPath());
			
			if (!ImageIO.write(scaled, format, saveDest)) {
				logger.error("Can't save to format: {} known formats {}",
						format, Arrays.toString(ImageIO.getWriterFormatNames()));
				throw new RuntimeException("Java runtime lacks support for saving to " + format);
			}
		} catch (IOException e) {
			logger.error("Failed to save image {} {}", fileName,
					e.getClass().getName() + ": " + e.getMessage());
			throw new HumanVisibleException("For some reason our computer couldn't save your photo. It's our fault; trying again later might help. If not, please let us know.");
		}
	}
	
	protected void writePhoto(BufferedImage scaled, String fileName, boolean overwrite) throws HumanVisibleException {
		
		doSave(scaled, fileName, overwrite, "png");
		
		if (getSaveJpegAlso()) {
			// Trying to use JPEG is probably a bad idea.
			// On many images, Java appears to produce a corrupt JPEG.
			// 
			// Unfortunately, we need to fall back to JPEG for flash 7.
			// So, what the hell. We'll always prefer the PNG except 
			// for flash 7 and maybe the JPEG will work when it's in a good
			// mood.
			//			
			// (someone on the internet says it's because scaling other than
			// NEAREST_NEIGHBOR puts an alpha channel in the BufferedImage,
			// which Java tries to save in the JPEG confusing most apps but
			// not Java's own JPEG loader - see link on wiki)
			//
			// We try stripping alpha in hopes the above theory is correct. 
			
			BufferedImage noAlpha =
				new BufferedImage(scaled.getWidth(), scaled.getHeight(),
					BufferedImage.TYPE_INT_RGB);
			Graphics2D g = noAlpha.createGraphics();
			g.drawImage(scaled, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
			g.dispose();

			doSave(noAlpha, fileName + ".jpg", overwrite, "jpeg");
		}
	}
	
	protected void writePhotos(Collection<BufferedImage> photos, String filename, boolean overwrite) throws IOException, HumanVisibleException {
		for (BufferedImage img : photos) {
			int size = img.getWidth(); // it's square, so w/h same difference
			String sizedName = size + "/" + filename;
			writePhoto(img, sizedName, true);
		}
	}
	
	private void startUpload(HttpServletRequest request, HttpServletResponse response, FileUpload upload, List items)
			throws HttpException, IOException, ServletException, HumanVisibleException {

		FileItem photo = null;
		
		String location;
		
		location = getRelativePath();
		
		logger.debug("uploading photo to {}", saveDir);

		Map<String,String> formParameters = new HashMap<String,String>();
		for (Object o : items) {
			FileItem item = (FileItem) o;
			if (item.isFormField()) {
				logger.debug("Form field {} = {}", item.getFieldName(), item.getString());
				formParameters.put(item.getFieldName(), item.getString());
			} else {
				photo = item;
			}
		}

		if (photo == null)
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "No photo uploaded?");
		
		User user = doLogin(request);
		
		reloadTo = formParameters.get("reloadTo");
		
		request.setAttribute("photoLocation", location);
		logger.debug("File {} size {} content-type " + photo.getContentType(), 
				photo.getName(), photo.getSize());
		doUpload(request, response, user, formParameters, photo);
	}
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException, ServletException, HumanVisibleException {
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
		upload.setSizeMax(MAX_FILE_SIZE);

		try {
				startUpload(request, response, upload, upload.parseRequest(request));
				return null;
		} catch (FileUploadException e) {
			// I don't have any real clue what this exception might be or indicate
			logger.warn("File upload exception, investigate what this was", e);
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "file upload malformed somehow; we aren't sure what went wrong");
		}
	}

	// Note that strict validation here is essential, since otherwise the user 
	// could ask for, say headshots/SOME_HIDDEN_FILE?size=../../..
	static final Pattern SIZE_PATTERN = Pattern.compile("[0-9]+(x[0-9]+)?"); 
	
	public static boolean isValidSize(String sizeString) {
		return SIZE_PATTERN.matcher(sizeString).matches();
	}
	
	public static String getEmbeddedSize(String noPrefix) {
		int lastSlash = noPrefix.lastIndexOf('/');
		if (lastSlash <= 0)
			return null;
		int nextToLastSlash = noPrefix.lastIndexOf('/', lastSlash-1);
		if (nextToLastSlash < 0)
			nextToLastSlash = -1; // Gets incremented to 0
		String potentialSize = noPrefix.substring(nextToLastSlash+1, lastSlash);
		if (isValidSize(potentialSize))
			return potentialSize;
		int nextSlash = noPrefix.indexOf('/');
		if (nextSlash <= 0)
			return null;
		
		potentialSize = noPrefix.substring(0, nextSlash);
		if (isValidSize(potentialSize))
			return potentialSize;
		return null;
	}
	
	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException {
		
		/* The request comes in one of several forms 
		 * 
		 *  /files/headshots/userid            default to size=60
		 *  /files/headshots/userid?size=48    generated by the client
		 *  /files/headshots/48/userid         another form of the above, used for web pages due to problems with query strings
		 *                                     in IE's AlphaImageLoader
		 *
		 * The filename on the server is saveDir/files/headshots/48/userid
		 * The default file is saveDir/files/headshots/48/default for example
		 */
		
		String defaultFilename = getDefaultFilename();
		String noPrefix = request.getPathInfo().substring(1); // Skip the leading slash
		
		String embeddedSize = getEmbeddedSize(noPrefix);
		// See if we need to insert a size into the request
		if (needsSize() && embeddedSize == null) {
			String size = getDefaultSize();
			String sizeParameter = request.getParameter("size");
			if (sizeParameter != null && isValidSize(sizeParameter))
				size = sizeParameter;
			if (embeddedSize != null)
				size = "" + embeddedSize;
			
			noPrefix = size + "/" + noPrefix;
		}
		
		File toServe = new File(saveDir, noPrefix);
		if (!toServe.exists()) {
			String parent = toServe.getParent();
			toServe = null;
			if (defaultFilename != null) {
				toServe = new File(parent, defaultFilename);
				if (!toServe.exists())
					toServe = null;
			}
		}

		if (toServe == null) {
			throw new HttpException(HttpResponseCode.NOT_FOUND, "No such image");
		}
		
		// If the requester passes a version with the URL, that's a signal that
		// it can be cached without checking for up-to-dateness. There's no
		// point in actually checking the version, so we don't
		if (request.getParameter("v") != null)
			setInfiniteExpires(response);
		
		// PNGs are saved with no extension at the moment. We don't 
		// ever save GIF and we only save JPEG as a fallback for themes
		// displayed in Flash versions less than 8
		String mimeType;
		String name = toServe.getName();
		if (name.endsWith(".gif"))
			mimeType = "image/gif";
		else if (name.endsWith(".jpeg"))
			mimeType = "image/jpeg";
		else
			mimeType = "image/png";
		
		sendFile(request, response, mimeType, toServe);
		
		return null;
	}
}
