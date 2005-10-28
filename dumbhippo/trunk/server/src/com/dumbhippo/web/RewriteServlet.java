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

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.web.CookieAuthentication.NotLoggedInException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

public class RewriteServlet extends HttpServlet {
	private static final Log logger = GlobalSetup.getLog(RewriteServlet.class);
	
	static final long serialVersionUID = 1;
	
	private Set<String> requiresSignin;
	private Set<String> jsfPages;
	private Set<String> htmlPages;
	
	private ServletContext context;
	
	private boolean hasSignin(HttpServletRequest request) {
		// First see if the user is already signed in to the session
		SigninBean signin;
		signin = SigninBean.getFromHttpSession(request.getSession());
		if (signin != null)
			return true;
		
		// If not, try to authenticate using cookies they've sent
		try {
			CookieAuthentication.authenticate(request);
			return true;
		} catch (BadTastingException e) {
			return false;
		} catch (NotLoggedInException e2) {
			return false;
		}
	}
	
	@Override
	public void service(HttpServletRequest request,	HttpServletResponse response) 
		throws IOException, ServletException {
		RequestDispatcher dispatcher = null;
		String newPath = null;
		
		String path = request.getServletPath();
		
		logger.debug("Handling request for" + path);
		
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
		
		if (jsfPages.contains(afterSlash)) {
			dispatcher = context.getNamedDispatcher("Faces Servlet");
			newPath = "/jsf" + path + ".jsp";
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
		
		jsfPages = new HashSet<String>();
		Set jsfPaths = context.getResourcePaths("/jsf/");
		if (jsfPaths != null) {
			for (Object o : jsfPaths) {
				String path = (String)o;
				if (path.endsWith(".jsp") && path.indexOf('/') != -1)
					jsfPages.add(path.substring(5, path.length() - 4));
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
