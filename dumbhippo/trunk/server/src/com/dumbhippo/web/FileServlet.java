package com.dumbhippo.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;


/**
 * This is a hack for now. In real life we'd probably let Apache take this over.
 * 
 * @author hp
 *
 */
public class FileServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;

	private static final Log logger = GlobalSetup.getLog(FileServlet.class);

	private Configuration config;
	private URI filesUri;
	private File filesDir;
	
	@Override
	public void init() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
			
		String filesUrl = config.getPropertyFatalIfUnset(HippoProperty.FILES_SAVEURL);
		
		try {
			filesUri = new URI(filesUrl);
		} catch (URISyntaxException e) {
			throw new RuntimeException("files url busted: " + filesUrl, e);
		}
		filesDir = new File(filesUri);
	}
	
	@Override
	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException {


	}

	private void copy(InputStream in, OutputStream out) throws IOException {

		byte[] buffer = new byte[256];
        int bytesRead;

        for (;;) {
        	bytesRead = in.read(buffer, 0, buffer.length);
        	if (bytesRead == -1)
        		break; // all done (NOT an error, that throws IOException)

            out.write(buffer, 0, bytesRead);
        }
	}
	
	@Override
	protected void wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException {
		String noPrefix = request.getRequestURI().replaceFirst("\\/files", "");
		File toServe = new File(filesDir, noPrefix);
		logger.debug("sending file " + toServe.getCanonicalPath());
		InputStream in = new FileInputStream(toServe);
		OutputStream out = response.getOutputStream();
		copy(in, out);
		out.flush();
		
		// set content type... but no good way to know what it is...
		response.setContentType("image/png");
	}
}
