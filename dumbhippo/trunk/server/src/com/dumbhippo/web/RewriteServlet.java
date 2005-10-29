package com.dumbhippo.web;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RewriteServlet extends HttpServlet {
	
	static final long serialVersionUID = 1;
	
	private Set<String> requiresSignin;
	private Set<String> jspPages;
	private Set<String> htmlPages;
	
	private ServletContext context;
	
	private boolean hasSignin(HttpServletRequest request) {
		return SigninBean.getForRequest(request).isValid();
	}
	
	@Override
	public void service(HttpServletRequest request,	HttpServletResponse response) 
		throws IOException, ServletException {
		RequestDispatcher dispatcher = null;
		String newPath = null;
		
		String path = request.getServletPath();
		
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
		// But that won't work since the Tomcat default servlet looks up the resource
		// based on servlet-path, which in the above will be missing /javascript
		// so we have to handle javascript/ and css/ URLs here.
		
		if (path.startsWith("/javascript/") || 
			path.startsWith("/css/")) {
			dispatcher = context.getNamedDispatcher("default");
			dispatcher.forward(request, response);
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
			dispatcher = context.getNamedDispatcher("jsp");
			newPath = "/jsp" + path + ".jsp";
		} else if (htmlPages.contains(afterSlash)) {
			dispatcher = context.getNamedDispatcher("default");
			newPath = "/html" + path + ".html";
		}
		
		if (dispatcher != null) {
			HttpServletRequest wrappedRequest = new RewrittenRequest(request, newPath);
			dispatcher.forward(wrappedRequest, response);
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
	}
}
