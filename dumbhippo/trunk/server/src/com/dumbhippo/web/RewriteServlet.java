package com.dumbhippo.web;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;

public class RewriteServlet extends HttpServlet {
	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(RewriteServlet.class);	
	
	static final long serialVersionUID = 1;
	
	private Set<String> requiresSignin;
	private Set<String> jspPages;
	private Set<String> htmlPages;
	private String buildStamp;
	
	private ServletContext context;

	private boolean hasSignin(HttpServletRequest request) {
		return SigninBean.getForRequest(request).isValid();
	}
	
    private String checkBuildStamp(String relativePath) {
		int firstSlash = relativePath.indexOf('/', 1);
		if (firstSlash < 0)
			return null;

		int buildStampLength = buildStamp.length();
		if (relativePath.regionMatches(firstSlash + 1, buildStamp, 0, buildStampLength)) {
			return (relativePath.substring(0, firstSlash) + relativePath.substring(firstSlash + 1 + buildStampLength));
		} else {
			return null;
		}
	}
    
	@Override
	public void service(HttpServletRequest request,	HttpServletResponse response) throws IOException, ServletException { 
		String newPath = null;
		
		String path = request.getServletPath();
		
		// logger.debug("Handling request for " + path);
		
		// The root URL is special-cased
		
		if (path.equals("/")) {
			if (hasSignin(request))
				response.sendRedirect(response.encodeRedirectURL("home"));
			else
				response.sendRedirect(response.encodeRedirectURL("main"));
			return;
		}
		
		// You'd think that we could handle static content with, in servlet-info.xml 
		// <servlet-mapping>
		//    <server-name>default</server-name>
		//    <url-pattern>/javascript/*</url-pattern>
		// </servlet-mapping>
		//
		// But that won't work since the Tomcat default servlet (and our modified version)
		// looks up the resource based on servlet-path, which in the above will be missing 
		// /javascript so we have to handle javascript/ and css/ URLs here. We could
		// hack StaticServlet further from the original code to avoid this.
		
		if (path.equals("/favicon.ico")) {
			// there are some cases where we aren't serving html so there's no 
			// <link type="icon"/> - we normally point browsers to /images/favicon.ico 
			// in the html itself, but we need this for when we don't
			RewrittenRequest rewrittenRequest = new RewrittenRequest(request, "/images/favicon.ico");
			context.getNamedDispatcher("default").forward(rewrittenRequest, response);
			return;
		} else if (path.startsWith("/javascript/") || 
			path.startsWith("/css/") ||
			path.startsWith("/images/")) {
			
			newPath = checkBuildStamp(path);
			if (newPath != null) {
				AbstractServlet.setInfiniteExpires(response);
				path = newPath;
			}
			
			if (path.equals("/javascript/config.js")) {
				// config.js is special and handled by a JSP
				context.getRequestDispatcher("/jsp/configjs.jsp").forward(request, response);
			} else if (newPath != null) {
				RewrittenRequest rewrittenRequest = new RewrittenRequest(request, newPath);
				context.getNamedDispatcher("default").forward(rewrittenRequest, response);
			} else {
				context.getNamedDispatcher("default").forward(request, response);
			}
			
			return;
		}
		
		String afterSlash = path.substring(1);
		
		// If this is a request to one of the pages configured as requiresLogin,
		// and the user isn't signed in, go to /signin, storing the real
		// destination in the query string. This only works for GET, since we'd
		// have to save the POST parameters somewhere.
		
		if (requiresSignin.contains(afterSlash) && 
			!hasSignin(request) && 
			request.getMethod().toUpperCase().equals("GET")) {
			String url = response.encodeRedirectURL("/signin?next=" + afterSlash);
			response.sendRedirect(url);
			return;
		}
			
		// Now handle the primary set of user visible pages, which is a merge
		// of static HTML and JSP's.
		
		if (jspPages.contains(afterSlash)) {
			// We can't use RewrittenRequest for JSP's because it breaks the
			// handling of <jsp:forward/> and is generally unreliable.
			newPath = "/jsp" + path + ".jsp";
			context.getRequestDispatcher(newPath).forward(request, response);
		} else if (htmlPages.contains(afterSlash)) {
			// We could eliminate the use of RewrittenRequest entirely by
			// adding a mapping for *.html to servlet-info.xml
			newPath = "/html" + path + ".html";
			RewrittenRequest rewrittenRequest = new RewrittenRequest(request, newPath);
			context.getNamedDispatcher("default").forward(rewrittenRequest, response);
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
	@Override
	public void init() throws ServletException {
		ServletConfig config = getServletConfig();
		context = config.getServletContext();
		
		requiresSignin = new HashSet<String>();
		String requiresSigninString = config.getInitParameter("requiresSignin");
		if (requiresSigninString != null)
			for (String page : requiresSigninString.split(","))
				requiresSignin.add(page);
		
		jspPages = new HashSet<String>();
		Set jspPaths = context.getResourcePaths("/jsp/");
		if (jspPaths != null) {
			for (Object o : jspPaths) {
				String path = (String)o;
				if (path.endsWith(".jsp") && path.indexOf('/') != -1)
					jspPages.add(path.substring(5, path.length() - 4));
			}
		}
		
		htmlPages = new HashSet<String>();
		Set htmlPaths = context.getResourcePaths("/html/");
		if (htmlPaths != null) {
			for (Object o : htmlPaths) {
				String path = (String)o;
				if (path.endsWith(".html") && path.indexOf('/') != -1)
					htmlPages.add(path.substring(6, path.length() - 5));
			}
		}		
		
        Configuration configuration = WebEJBUtil.defaultLookup(Configuration.class);
        buildStamp = configuration.getProperty(HippoProperty.BUILDSTAMP);
        
        // We store the builtstamp in the servlet context so we can reference it from JSP pages
        getServletContext().setAttribute("buildStamp", buildStamp);
	}
}
