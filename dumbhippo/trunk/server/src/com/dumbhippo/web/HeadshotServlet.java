package com.dumbhippo.web;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

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
				logger.debug("File " + item.getName() + " size " + item.getSize() + " content-type " + item.getContentType());
				
				if (!item.getContentType().equals("image/png")) {
					// we have no way to record the mime type for when we serve the file
					throw new HttpException(HttpResponseCode.BAD_REQUEST, "we only do PNG for now");
				}
				
				File saveDest = new File(headshotSaveDir, user.getId());
				
				try {
					logger.debug("saving to " + saveDest.getCanonicalPath());
					item.write(saveDest);
				} catch (Exception e) {
					logger.error(e);
					throw new HttpException(HttpResponseCode.INTERNAL_SERVER_ERROR, "failed to save photo", e);
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
		xml.append("<a href=\"/home\">Go to your page</a>");
		xml.append("</body>\n</html>\n");
		
		OutputStream out = response.getOutputStream();
		out.write(xml.toString().getBytes());
		out.flush();
	}
	
	@Override
	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException {
		DiskFileUpload upload = new DiskFileUpload();
		
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
