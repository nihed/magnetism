package com.dumbhippo.web;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;

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
	
	protected abstract void doUpload(HttpServletRequest request, HttpServletResponse response, Person person, Map<String,String> params, FileItem photo)
	throws HttpException, IOException, ServletException, HumanVisibleException;
	
	protected void writePhoto(BufferedImage scaled, String fileName, boolean overwrite) throws IOException, HumanVisibleException {
		File saveDest = new File(saveDir, fileName);
		if (!overwrite && saveDest.exists())
			throw new IOException("Can't overwrite existing file");
		if (logger.isDebugEnabled())
			logger.debug("saving to {}", saveDest.getCanonicalPath());

		// FIXME this should be JPEG, but Java appears to fuck that up
		// on a lot of images and produce a corrupt image
		// (someone on the internet says it's because scaling other than
		// NEAREST_NEIGHBOR puts an alpha channel in the BufferedImage,
		// which Java tries to save in the JPEG confusing most apps but
		// not Java's own JPEG loader - see link on wiki)
		if (!ImageIO.write(scaled, "png", saveDest)) {
			throw new HumanVisibleException("For some reason our computer couldn't save your photo. It's our fault; trying again later might help. If not, please let us know.");
		}		
	}
	
	protected void writePhotos(Collection<BufferedImage> photos, String filename, boolean overwrite) throws IOException, HumanVisibleException {
		for (BufferedImage img : photos) {
			int size = img.getWidth(); // it's square, so w/h same difference
			String sizedName = size + "/" + filename;
			writePhoto(img, sizedName, true);
		}
	}
	
	private void startUpload(HttpServletRequest request, HttpServletResponse response, DiskFileUpload upload, List items)
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
		
		Person user = doLogin(request);
		if (user == null)
			throw new HttpException(HttpResponseCode.FORBIDDEN, "You must be logged in to change a photo");
		
		reloadTo = formParameters.get("reloadTo");
		
		request.setAttribute("photoLocation", location);
		logger.debug("File {} size {} content-type " + photo.getContentType(), 
				photo.getName(), photo.getSize());
		doUpload(request, response, user, formParameters, photo);
	}
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException, ServletException, HumanVisibleException {
		DiskFileUpload upload = new DiskFileUpload();
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

	@Override
	protected void wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException {
		
		/* This is a little confusing. The web request is for something like 
		 *  /files/headshots/48/userid
		 * The filename on the server is saveDir/files/headshots/48/userid
		 * The default file is saveDir/files/headshots/48/default for example
		 */
		
		String defaultFilename = getDefaultFilename();
		String noPrefix = request.getPathInfo().substring(1); // Skip the leading slash
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
		
		sendFile(request, response, "image/png", toServe);
	}
}
