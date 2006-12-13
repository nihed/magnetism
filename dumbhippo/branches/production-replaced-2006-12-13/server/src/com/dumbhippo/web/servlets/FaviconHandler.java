package com.dumbhippo.web.servlets;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.util.FaviconCache;
import com.dumbhippo.server.util.FaviconCache.Icon;
import com.dumbhippo.web.RewrittenRequest;
import com.dumbhippo.web.WebEJBUtil;
import com.dumbhippo.web.servlets.AbstractServlet.HttpResponseCode;

/**
 * This isn't a servlet, just a helper to keep some cruft out of RewriteServlet.
 * The idea was to let RewriteServlet do the build stamp handling. It ended up 
 * a little messy though. Since we also need the build stamp outside RewriteServlet 
 * in a bunch of other places (search for /images[0-9]?/ in *.java for example) 
 * maybe we should just factor out a build stamp class that can be used to get the
 * stamp and remove it from urls. Though, dh:png does munge in the buildstamp 
 * for stuff with the right prefix.
 */
public class FaviconHandler {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup
			.getLogger(FaviconHandler.class);	
	
	private FeedSystem feedSystem;
	
	public FaviconHandler() {
		feedSystem = WebEJBUtil.defaultLookup(FeedSystem.class);
	}
	
	// this is NOT in a transaction
	public void service(ServletContext context, HttpServletRequest request,	HttpServletResponse response, String pathWithoutBuildStamp, boolean hadBuildStamp)
		throws IOException, ServletException {
		String method = request.getMethod().toUpperCase();
		boolean headOnly = false;
		if (method.equals("HEAD"))
			headOnly = true;
		// RewriteServlet should have allowed only GET or POST to here. 
		// POST is kind of broken, but just allow it, not worth constructing the 
		// right kind of error reply here for now
	
		String defaultIcon = null;
		String faviconUrl = null;
		if (pathWithoutBuildStamp.startsWith("/favicons/feed/")) {
			
			defaultIcon = "/images3/feed_icon16x16.png";
			
			String feedSourceId = pathWithoutBuildStamp.substring("/favicons/feed/".length());
			try {
				// this will create a transaction
				faviconUrl = feedSystem.getUrlToScrapeFaviconFrom(feedSourceId);
			} catch (NotFoundException e) {
				logger.debug("No favicon url found for feed: {} (link id='{}')", e.getMessage(), feedSourceId);
			}
		} else {
			logger.debug("Don't know how to find favicon for path {}", pathWithoutBuildStamp);
		}
		
		Icon icon = null;
		if (faviconUrl != null) {
			FaviconCache cache = FaviconCache.getInstance();
			icon = cache.loadIconForPage(faviconUrl);
		}
		
		if (icon == null || !icon.getLoaded()) {
			logger.debug(" failed to load favicon '{}' default fallback '{}'", faviconUrl, defaultIcon);
			if (defaultIcon != null) {
				RewrittenRequest rewrittenRequest = new RewrittenRequest(request, defaultIcon);
				context.getNamedDispatcher("default").forward(rewrittenRequest, response);
			} else {
				HttpResponseCode.NOT_FOUND.send(response, "Unknown favicon");
			}
		} else {
			logger.debug(" got favicon icon type {}, {} bytes", icon.getContentType(), icon.getIconData().length);
			
			if (hadBuildStamp)
				AbstractServlet.setInfiniteExpires(response);
			
			response.setContentType(icon.getContentType());
			response.setContentLength(icon.getIconData().length);
			
			if (!headOnly) {
				OutputStream out = response.getOutputStream();
				out.write(icon.getIconData());
				out.close();
			}
		}
	}
}
