package com.dumbhippo.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;

public class RewriteServlet extends HttpServlet {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(RewriteServlet.class);	
	
	static final long serialVersionUID = 1;
	
	private boolean stealthMode;
	private Set<String> requiresSignin;
	private Set<String> requiresSigninStealth;
	private Set<String> noSignin;
	private Set<String> jspPages;
	private Set<String> htmlPages;
	private String buildStamp;
	
	private List<String> psaLinks; // used to choose a random one
	private int nextPsa;
	
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
     
    private synchronized String getNextPsa() {
    	if (nextPsa >= psaLinks.size())
    		nextPsa = 0;
    	String s = psaLinks.get(nextPsa);
    	++nextPsa;
    	return s;
    }
    
	@Override
	public void service(HttpServletRequest request,	HttpServletResponse response) throws IOException, ServletException {
		
		String newPath = null;
		
		String path = request.getServletPath();
		
		// logger.debug("Handling request for " + path);
		
		// The root URL is special-cased, we redirect it depending
		// on whether the user is signed in and depending on our
		// configuration. Note that we don't use encodeRedirectURL()
		// here because that will add ;jsessionid=1230811241...
		// when page is accessed without an existing session, which is 
		// silly, since we don't support cookie-less operation anywhere
		// else.
		
		if (path.equals("/")) {
			if (hasSignin(request))
				response.sendRedirect("home");
			else if (stealthMode)
				response.sendRedirect("comingsoon");
			else
				response.sendRedirect("main");
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
			path.startsWith("/images/") ||
			path.startsWith("/flash/")) {
			
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
		// and the user isn't signed in, go to /who-are-you, storing the real
		// destination in the query string. This only works for GET, since we'd
		// have to save the POST parameters somewhere.
		
		if ((requiresSignin.contains(afterSlash) || (stealthMode && requiresSigninStealth.contains(afterSlash))) && 
			!hasSignin(request) && 
			request.getMethod().toUpperCase().equals("GET")) { 
			String url = response.encodeRedirectURL("/who-are-you?next=" + afterSlash);
			if (stealthMode && requiresSigninStealth.contains(afterSlash)) {
				url = url + "&wouldBePublic=true";
			}
			response.sendRedirect(url);
			return;
		}
			
		// Now handle the primary set of user visible pages, which is a merge
		// of static HTML and JSP's.
		
		// Set variables with rotating PSAs; it's important to avoid 
		// using up "getNextPsa()" with pages that won't use these though,
		// so we get the full rotation. e.g. with 3 PSAs, if you getNextPsa()
		// when loading 2 PSAs on the page itself, then each page load goes through 6 PSAs which
		// means you always skip certain ones on the JSP
		if (!afterSlash.startsWith("psa-")) {
			request.setAttribute("psa1", getNextPsa());
			request.setAttribute("psa2", getNextPsa());
		}
		
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
	
	static private Set<String> getStringSet(ServletConfig config, String param) {
		Set<String> set = new HashSet<String>();
		String s = config.getInitParameter(param);
		if (s != null)
			for (String m : s.split(","))
				set.add(m.trim());
		return set;
	}
	
	@Override
	public void init() throws ServletException {
		ServletConfig config = getServletConfig();
		context = config.getServletContext();

		stealthMode = true;
		String stealthModeString = config.getInitParameter("stealthMode");
		if (stealthModeString != null)
			stealthMode = Boolean.parseBoolean(stealthModeString);
		
		requiresSignin = getStringSet(config, "requiresSignin");
		requiresSigninStealth = getStringSet(config, "requiresSigninStealth");
		noSignin = getStringSet(config, "noSignin");
		
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
	
		Set<String> withSigninRequirements = new HashSet<String>(requiresSignin);
		withSigninRequirements.addAll(requiresSigninStealth);
		withSigninRequirements.addAll(noSignin);
		
		for (String p : jspPages) {
			if (!withSigninRequirements.contains(p)) {
				if (p.startsWith("psa-"))
					noSignin.add(p);
				else {
					logger.warn(".jsp " + p + " does not have its signin requirements specified");
					requiresSigninStealth.add(p);
				}
			}
		}

		for (String p : htmlPages) {
			if (!withSigninRequirements.contains(p)) {
				if (p.startsWith("psa-"))
					noSignin.add(p);
				else {
					logger.warn(".html " + p + " does not have its signin requirements specified");
					requiresSigninStealth.add(p);
				}
			}
		}
		
		for (String p : withSigninRequirements) {
			if (!jspPages.contains(p) && !htmlPages.contains(p)) {
				logger.warn("Page '" + p + "' in servlet config is not a .jsp or .html we know about");
			}
		}
		
		psaLinks = new ArrayList<String>();
		for (String p : noSignin) {
			if (p.startsWith("psa-"))
				psaLinks.add(p);
		}
		logger.debug("Added " + psaLinks.size() + " PSAs: " + psaLinks);
		
        Configuration configuration = WebEJBUtil.defaultLookup(Configuration.class);
        buildStamp = configuration.getProperty(HippoProperty.BUILDSTAMP);
        
        // We store the builtstamp in the servlet context so we can reference it from JSP pages
        getServletContext().setAttribute("buildStamp", buildStamp);
	}
}
