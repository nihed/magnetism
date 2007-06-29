package com.dumbhippo.web.servlets;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.applications.ApplicationIconView;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.web.WebEJBUtil;

public class ApplicationIconServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;

	static final int DEFAULT_SIZE = 48;
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(ApplicationIconServlet.class);

	private Configuration config;
	private ApplicationSystem applicationSystem;
	private File saveDir;
	
	@Override
	public void init() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
		applicationSystem = WebEJBUtil.defaultLookup(ApplicationSystem.class);
		
		String filesUrl = config.getPropertyFatalIfUnset(HippoProperty.FILES_SAVEURL);
		String saveUrl = filesUrl + Configuration.APPICONS_RELATIVE_PATH;
		
		URI saveUri;
		try {
			saveUri = new URI(saveUrl);
		} catch (URISyntaxException e) {
			throw new RuntimeException("save url busted", e);
		}
		saveDir = new File(saveUri);
	}	
	
	// getPathInfo() seems to include the query string against the J2EE docs;
	// this may be Tomcat being bug-compatible with the Sun J2EE implementation
	static final Pattern PATH_INFO_REGEX = Pattern.compile("(?:([0-9]+)/)?([A-Za-z0-9-.]+)(?:.png|.PNG)?");
	
	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException {
		
		/* We support either 
		 * 
		 *  /files/appicons/<appname>           default to size=48
		 *  /files/headshots/<appname>&size=48  choose size 48
		 *  /files/headshots/48/userid          old form, supported for consistency
		 */
		
		if (request.getPathInfo() == null)
			throw new HttpException(HttpResponseCode.NOT_FOUND, "No image specified");
		
		String noPrefix = request.getPathInfo().substring(1); // Skip the leading slash
		
		Matcher m = PATH_INFO_REGEX.matcher(noPrefix);
		if (!m.matches()) {
			throw new HttpException(HttpResponseCode.NOT_FOUND, "Don't understand the requested application icon");
		}
		
		String sizeString = m.group(1);
		String appId = m.group(2);
		
		if (sizeString == null)
			sizeString = request.getParameter("size");
		
		int size;
		
		try {
			size = Integer.parseInt(sizeString);
		} catch (NumberFormatException e) {
			size = DEFAULT_SIZE;
		}
		
		if (size <= 0)
			size = DEFAULT_SIZE;

		ApplicationIconView iconView;
		try {
			iconView = applicationSystem.getIcon(appId, size);
		} catch (NotFoundException e) {
			// FIXME: We really should be using a default icon instead
			throw new HttpException(HttpResponseCode.NOT_FOUND, "No such image");
		}

		File toServe = new File(saveDir, iconView.getIcon().getIconKey() + ".png");
		
		// If the requester passes a version with the URL, that's a signal that
		// it can be cached without checking for up-to-dateness. There's no
		// point in actually checking the version, so we don't
		if (request.getParameter("v") != null)
			setInfiniteExpires(response);
		
		sendFile(request, response, "image/png", toServe);
		
		return null;
	}
	
	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		return false;
	}
}
