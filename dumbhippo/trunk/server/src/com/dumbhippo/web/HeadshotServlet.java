package com.dumbhippo.web;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;

public class HeadshotServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;

	private static final Log logger = GlobalSetup.getLog(HeadshotServlet.class);

	private static final int HEADSHOT_DIMENSION = 48;
	private static final int MAX_FILE_SIZE = 1024 * 1024 * 5; // 5M is huge, but photos can be big...
	// scaling something huge is probably bad, but allowing a typical desktop background is 
	// good if we can handle it...
	private static final int MAX_IMAGE_DIMENSION = 2048;
	
	private Configuration config;
	private URI headshotSaveUri;
	private File headshotSaveDir;
	
	@Override
	public void init() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
		
		String filesUrl = config.getPropertyFatalIfUnset(HippoProperty.FILES_SAVEURL);
		String headshotsUrl = filesUrl + Configuration.HEADSHOTS_RELATIVE_PATH;
		
		try {
			headshotSaveUri = new URI(headshotsUrl);
		} catch (URISyntaxException e) {
			throw new RuntimeException("headshot save url busted: " + headshotsUrl, e);
		}
		headshotSaveDir = new File(headshotSaveUri);
	}
	
	private void doHeadshot(HttpServletRequest request, HttpServletResponse response, DiskFileUpload upload, List items)
			throws HttpException, IOException {

		logger.debug("uploading headshot");

		Person user = doLogin(request, response, true);
		if (user == null)
			throw new HttpException(HttpResponseCode.FORBIDDEN, "You must be logged in to upload a headshot");

		for (Object o : items) {
			FileItem item = (FileItem) o;
			if (item.isFormField()) {
				logger.debug("Form field " + item.getFieldName() + " = " + item.getString());
			} else {
				logger.debug("File " + item.getName() + " size " + item.getSize() + " content-type "
						+ item.getContentType());

				InputStream in = item.getInputStream();

				BufferedImage image = ImageIO.read(in);
				if (image == null) {
					throw new HttpException(HttpResponseCode.BAD_REQUEST, "Could not load your image");
				}
				if (image.getWidth() > MAX_IMAGE_DIMENSION || image.getHeight() > MAX_IMAGE_DIMENSION) {
					throw new HttpException(HttpResponseCode.BAD_REQUEST, "Your image is too big");
				}

				// FIXME It would be nicer to only pad images so they are always square and our 
				// css won't mangle their aspect ratio. I think to do this we have to manually
				// create the destination BufferedImage and then transform onto it.
				
				// FIXME it would be nicer to avoid round-tripping a jpeg through a BufferedImage

				double scaleX, scaleY;
				if (image.getHeight() > image.getWidth()) {
					scaleY = HEADSHOT_DIMENSION / (double) image.getHeight();
					scaleX = scaleY;
				} else {
					scaleX = HEADSHOT_DIMENSION / (double) image.getWidth();
					scaleY = scaleX;
				}

				logger.debug("Scaling headshot scaleX = " + scaleX + " scaleY = " + scaleY + "new width = "
						+ image.getWidth() * scaleX + " new height = " + image.getHeight() * scaleY);

				AffineTransform tx = new AffineTransform();
				tx.scale(scaleX, scaleY);
				AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
				BufferedImage scaled = op.filter(image, null);
				File saveDest = new File(headshotSaveDir, user.getId());
				logger.debug("saving to " + saveDest.getCanonicalPath());

				// FIXME this should be JPEG, but Java appears to fuck that up
				// on a lot
				// of images and produce a corrupt image
				if (!ImageIO.write(scaled, "png", saveDest)) {
					throw new HttpException(HttpResponseCode.INTERNAL_SERVER_ERROR, "Failed to save image");
				}
			}
		}

		response.setContentType("text/html");
		XmlBuilder xml = new XmlBuilder();
		xml.appendHtmlHead("Your Photo");
		xml.append("<body>\n<p>Your new photo looks like this:</p>");
		xml.append("<img src=\"");
		xml.appendEscaped("/files");
		xml.appendEscaped(Configuration.HEADSHOTS_RELATIVE_PATH);
		xml.appendEscaped("/" + user.getId());
		xml.append("\"/>\n");
		xml.append("<p>(If this is your old photo, try pressing reload.)</p>");
		xml.append("<p><a href=\"/home\">Go to your page</a></p>");
		xml.append("<p><a href=\"/myphoto\">Change to another photo</a></p>");
		xml.append("</body>\n</html>\n");

		OutputStream out = response.getOutputStream();
		out.write(xml.toString().getBytes());
		out.flush();
	}
	
	@Override
	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException {
		DiskFileUpload upload = new DiskFileUpload();
		upload.setSizeMax(MAX_FILE_SIZE);
		
		try {
			List items = upload.parseRequest(request);

			if (request.getRequestURI().equals("/upload" + Configuration.HEADSHOTS_RELATIVE_PATH)) {
				doHeadshot(request, response, upload, items);
			} else {
				throw new HttpException(HttpResponseCode.NOT_FOUND, "No upload page " + request.getRequestURI());
			}
			
		} catch (FileUploadException e) {
			// I don't have any real clue what this exception might be or indicate
			logger.error("File upload exception", e);
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "file upload malformed");
		}
	}

	@Override
	protected void wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException {
		logger.debug("HeadshotServlet doesn't do anything on GET");
	}	
}
