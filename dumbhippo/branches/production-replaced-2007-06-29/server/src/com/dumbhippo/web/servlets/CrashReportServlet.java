package com.dumbhippo.web.servlets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Properties;

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
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.web.WebEJBUtil;

public class CrashReportServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = GlobalSetup.getLogger(CrashReportServlet.class);
	
	// Our typical dump size seems to be about 20k
	private static final int MAX_IN_MEMORY_SIZE = 32 * 1024;
	private static final long MAX_FILE_SIZE = 256 * 1024;
	
	private Configuration config;
	private File crashdumpDir;

	@Override
	public void init() throws ServletException {
		super.init();
		config = WebEJBUtil.defaultLookup(Configuration.class);
		try {
			crashdumpDir = new File(config.getPropertyNoDefault(HippoProperty.CRASHDUMP_DIR));
		} catch (PropertyNotFoundException e) {
			throw new RuntimeException("crashdumpDir property was not set in super configuration");
		}
	}
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException {
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(MAX_IN_MEMORY_SIZE);
		
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
		upload.setSizeMax(MAX_FILE_SIZE);
		
		Properties properties = new Properties();
		
		String reportId = Guid.createNew().toString();
		File dumpFile = new File(crashdumpDir, reportId + ".dmp");
		File propertiesFile = new File(crashdumpDir, reportId + ".properties");
		
		Collection<?> items;
		try {
			items = upload.parseRequest(request);
		} catch (FileUploadException e) {
			logger.warn("Error parsing crash report upload");
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "file upload malformed somehow; we aren't sure what went wrong");
		}
		
		for (Object o : items) {
			FileItem item = (FileItem) o;
			String name = item.getFieldName();
			
			if (name.equals("version") || name.equals("platform")) {
				String value;
				try {
					value = item.getString("UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException("UTF-8 is too supported");
				}
				
				if (name.equals("platform") || name.equals("version"))
					properties.setProperty(name, value);
			} else if (name.equals("upload_file_minidump")) {
				try {
					item.write(dumpFile);
				} catch (Exception e) {
					logger.warn("Couldn't write crash dump to disk", e);
					throw new HttpException(HttpResponseCode.INTERNAL_SERVER_ERROR, "Couldn't save crash report");
				}
			}
		}
		
		User user = getUser(request);
		if (user != null)
			properties.setProperty("user", user.getId());
		
		properties.setProperty("uploadDate", Long.toString(System.currentTimeMillis()));
		properties.setProperty("sendingIp", request.getRemoteAddr());
		
		try {
			OutputStream propertiesOut = new FileOutputStream(propertiesFile);
			properties.store(propertiesOut, null);
			propertiesOut.close();
		} catch (IOException e) {
			logger.warn("Couldn't write crash dump properties to disk", e);
			throw new HttpException(HttpResponseCode.INTERNAL_SERVER_ERROR, "Couldn't save crash report");
		}
		
		response.setContentType("text/plain; charset=UTF-8");
		
		OutputStream responseOut = response.getOutputStream();
		responseOut.write(reportId.getBytes("UTF-8"));
		responseOut.close();
		
		logger.debug("Crash dump succesfully written to {}", dumpFile.getPath());
		
		return null;
	}
	
	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		return false;
	}
}
