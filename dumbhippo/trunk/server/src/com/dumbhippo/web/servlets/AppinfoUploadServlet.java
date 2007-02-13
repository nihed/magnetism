package com.dumbhippo.web.servlets;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.applications.AppinfoFile;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.web.WebEJBUtil;

public class AppinfoUploadServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = GlobalSetup.getLogger(CrashReportServlet.class);
	
	// Our typical dump size seems to be about 20k
	private static final int MAX_IN_MEMORY_SIZE = 32 * 1024;
	private static final long MAX_FILE_SIZE = 256 * 1024;
	
	private Configuration config;
	private ApplicationSystem applicationSystem;
	private IdentitySpider identitySpider;
	private File appinfoDir;

	@Override
	public void init() throws ServletException {
		super.init();
		applicationSystem = WebEJBUtil.defaultLookup(ApplicationSystem.class);
		config = WebEJBUtil.defaultLookup(Configuration.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		try {
			appinfoDir = new File(config.getPropertyNoDefault(HippoProperty.APPINFO_DIR));
		} catch (PropertyNotFoundException e) {
			throw new RuntimeException("appinfoDir property was not set in super configuration");
		}
	}

	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException {
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(MAX_IN_MEMORY_SIZE);
		
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
		upload.setSizeMax(MAX_FILE_SIZE);
		
		User user = getUser(request);
		// isAdminstrator re-attaches
		if (user == null || !identitySpider.isAdministrator(user))
			throw new HttpException(HttpResponseCode.FORBIDDEN, "you don't have permission to upload application info files");
		
		Guid uploadId = Guid.createNew();
		
		File saveLocation = new File(appinfoDir, uploadId.toString() + ".dappinfo");
		
		Collection<?> items;
		try {
			items = upload.parseRequest(request);
		} catch (FileUploadException e) {
			logger.warn("Error parsing appinfo file upload");
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "file upload malformed somehow; we aren't sure what went wrong");
		}
		
		for (Object o : items) {
			FileItem item = (FileItem) o;
			String name = item.getFieldName();
			
			if (name.equals("appinfo_file")) {
				try {
					item.write(saveLocation);
				} catch (Exception e) {
					logger.warn("Couldn't write appinfo dump to disk", e);
					throw new HttpException(HttpResponseCode.INTERNAL_SERVER_ERROR, "Couldn't save appinfo file");
				}
				
				break;
			}
		}
		
		AppinfoFile file;
		try {
			file = new AppinfoFile(saveLocation);
		} catch (IOException e) {
			saveLocation.delete();
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Error reading appinfo file: " + e.getMessage());
		} catch (ValidationException e) {
			saveLocation.delete();
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "appinfo file didn't validate: " + e.getMessage());
		}

		try {
			applicationSystem.addUpload(user.getGuid(), uploadId, file);
		} catch (RuntimeException e) {
			file.close();
			saveLocation.delete();
			throw e;
		}
		
		file.close();
		logger.debug("Appinfo file succesfully written to {}", saveLocation.getPath());
		
		return null;
	}
	
	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		// We don't want to hold a transaction while processing the upload
		return false;
	}
}
