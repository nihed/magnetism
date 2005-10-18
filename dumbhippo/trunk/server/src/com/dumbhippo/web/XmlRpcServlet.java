package com.dumbhippo.web;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.apache.commons.logging.Log;
import org.apache.xmlrpc.XmlRpcServer;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.AjaxGlueHttp;
import com.dumbhippo.server.AjaxGlueXmlRpc;
import com.dumbhippo.server.HttpContentTypes;
import com.dumbhippo.server.HttpParams;
import com.dumbhippo.web.EjbLink.NotLoggedInException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

public class XmlRpcServlet extends HttpServlet {

	private static final Log logger = GlobalSetup.getLog(XmlRpcServlet.class);
	
	private static final long serialVersionUID = 0L;

	// mappings from content type to url, like "/xml/foobar", "/text/baz" lets us do the same 
	// "API" in different formats
	private static final String[][] TYPE_DIR_MAPPINGS = { { "text/xml", "xml" }, {"text/plain", "text" } };
	
	private static final String XMLRPC_KEY = "org.dumbhippo.web.XmlRpcServlet.XmlRpcServer";

	enum HttpResponseCode {
		
		// There are a lot more response codes we aren't wrapping here
		
		OK(HttpServletResponse.SC_OK),
		
		// This means the form submitted OK but there's no reply data
		NO_CONTENT(HttpServletResponse.SC_NO_CONTENT),
		
		MOVED_PERMANENTLY(HttpServletResponse.SC_MOVED_PERMANENTLY),
		
		NOT_FOUND(HttpServletResponse.SC_NOT_FOUND),
		
		INTERNAL_SERVER_ERROR(HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
	
		FORBIDDEN(HttpServletResponse.SC_FORBIDDEN),
		
		// this means "we are hosed for a minute, try back in a bit"
		// There's a Retry-After header we could set...
		SERVICE_UNAVAILABLE(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		
		private int code;
		HttpResponseCode(int code) {
			this.code = code;
		}
		
		void send(HttpServletResponse response, String message) throws IOException {
			response.sendError(code, message);
		}
	}
	
	private static class HttpException extends Exception {
		
		private static final long serialVersionUID = 0L;
		private HttpResponseCode httpResponseCode;
		
		HttpException(HttpResponseCode httpResponseCode, String message, Throwable cause) {
			super(message, cause);
			this.httpResponseCode = httpResponseCode;
		}
		
		HttpException(HttpResponseCode httpResponseCode, String message) {
			super(message);
			this.httpResponseCode = httpResponseCode;
		}
		
		void send(HttpServletResponse response) throws IOException {
			httpResponseCode.send(response, getMessage());
		}	
	}
	
	private String[] parseUri(String uri) throws HttpException {
		
		if (uri.length() < 4) // "/a/b".length() == 4
			throw new HttpException(HttpResponseCode.NOT_FOUND,
					"URI is too short to be valid, should be of the form /typeprefix/methodname, e.g. /xml/frobate");
		
		// ignore trailing /
		if (uri.endsWith("/"))
			uri = uri.substring(0, uri.length()-1);
		
		// split into the two components, the result is { "", "xml", "frobate" }
		String[] ret = uri.split("/");
		if (ret.length != 3)
			throw new HttpException(HttpResponseCode.NOT_FOUND,
					"All URIs are of the form /typeprefix/methodname, e.g. /xml/frobate, split into: " + Arrays.toString(ret));
		
		return new String[] { ret[1], ret[2] };
	}
	
	private <T> void invokeHttpRequest(T object, HttpServletRequest request, HttpServletResponse response) throws IOException, HttpException {
		String requestedContentType = request.getContentType();
		String requestUri = request.getRequestURI();
		Method[] methods = object.getClass().getMethods();
		
		String[] uriComponents = parseUri(requestUri);
		String typeDir = uriComponents[0];
		String requestedMethod = uriComponents[1];
		
		boolean ok = false;
		for (String[] pair : TYPE_DIR_MAPPINGS) {
			if (requestedContentType == null) {
				if (typeDir.equals(pair[1])) {
					requestedContentType = pair[0];
					ok = true;
					break;
				}        
			} else if (requestedContentType.equals(pair[0]) && typeDir.equals(pair[1])) {
				ok = true;
				break;
			}
		}
		if (!ok) {
			throw new HttpException(HttpResponseCode.NOT_FOUND, "Must have content type matching directory path, allowed are "+ TYPE_DIR_MAPPINGS);
		}
		
		for (Method m : methods) {
			HttpContentTypes contentAnnotation = m.getAnnotation(HttpContentTypes.class);
			HttpParams paramsAnnotation = m.getAnnotation(HttpParams.class);
			
			if (contentAnnotation == null)
				continue;

			if (!m.getName().toLowerCase().equals(requestedMethod))
				continue;
			
			if (m.getReturnType() != null)
				throw new RuntimeException("HTTP method " + m.getName() + " must return void not " + m.getReturnType().getCanonicalName());
			
			String replyContentType = null;
			for (String t : contentAnnotation.value()) {
				if (t.equals(requestedContentType)) {
					replyContentType = t;
					break;
				}
			} 
			
			if (replyContentType == null) {
				throw new HttpException(HttpResponseCode.NOT_FOUND,
						"Wrong content type requested " + requestedContentType + " valid types for method are " + contentAnnotation.value());
			}
			
			Class<?> args[] = m.getParameterTypes();
			if (args.length != 2 + paramsAnnotation.value().length) {
				throw new RuntimeException("HTTP method " + m.getName() + " should have params OutputStream,String then " + paramsAnnotation.value());
			}
			if (!OutputStream.class.isAssignableFrom(args[0])) {
				throw new RuntimeException("HTTP method " + m.getName() + " must have OutputStream as arg 0");
			}
			if (!String.class.isAssignableFrom(args[1])) {
				throw new RuntimeException("HTTP method " + m.getName() + " must have String contentType as arg 1");
			}
			Object[] toPassIn = new Object[args.length];
			
			OutputStream out = response.getOutputStream();
			
			toPassIn[0] = out;
			toPassIn[1] = replyContentType;
			
			int i = 2;
			for (String pname : paramsAnnotation.value()) {
				if(!String.class.isAssignableFrom(args[i].getClass()))
					throw new RuntimeException("Only args of type String supported for now");
				  
				toPassIn[i] = request.getParameter(pname);
				
				++i;
			}
			
			response.setContentType(replyContentType);
			
			try {
				logger.debug("Invoking method " + m.getName() + " with args " + Arrays.toString(toPassIn));
				m.invoke(object, toPassIn);
			} catch (IllegalArgumentException e) {
				logger.error(e);
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				logger.error(e);
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				logger.error(e);
				throw new RuntimeException(e);
			}
			
			out.flush();
		}
	}
	
	static class SessionBoundXmlRpcServer extends XmlRpcServer implements HttpSessionActivationListener {
		public void sessionWillPassivate(HttpSessionEvent event) {
			// Remove ourselves, since we're just a transient cache
			HttpSession session = event.getSession();
			synchronized(session) {
				session.setAttribute(XMLRPC_KEY, null);
			}
		}

		public void sessionDidActivate(HttpSessionEvent event) {
			// nothing to do, servlet will re-create us on demand
		}
	}
	
	private XmlRpcServer getSessionXmlRpc(HttpServletRequest request) throws HttpException {
		HttpSession session = request.getSession();
		XmlRpcServer xmlrpc;
	
		synchronized(session) {
			xmlrpc = (XmlRpcServer) session.getAttribute(XMLRPC_KEY);
			if (xmlrpc == null) {
				xmlrpc = new SessionBoundXmlRpcServer();
				
				// Java thread locks are recursive so this is OK...
				AjaxGlueXmlRpc glue = getSessionGlueXmlRpc(request);		
				
				// glue is a proxy that only exports the one interface, 
				// so safe to export it all
				xmlrpc.addHandler("dumbhippo", glue);
				
				session.setAttribute(XMLRPC_KEY, xmlrpc);
			}
		}
		
		return xmlrpc;
	}
	
	private EjbLink getLoggedInEjb(HttpServletRequest request) throws HttpException {
		EjbLink ejb = EjbLink.getForSession(request.getSession());

		try {
			ejb.attemptLogin(request);
		} catch (BadTastingException e) {
			// In an HTML servlet, we would redirect to a login page; but in this
			// servlet we can't do much, we have no UI

			logger.debug(e);
			throw new HttpException(HttpResponseCode.FORBIDDEN, "Authorization failed, please log in again (bad cookie)", e);
		} catch (NotLoggedInException e) {
			// In an HTML servlet, we would redirect to a login page; but in this
			// servlet we can't do much, we have no UI
			logger.debug(e);
			throw new HttpException(HttpResponseCode.FORBIDDEN, "You need to log in", e);
		}
		return ejb;
	}
	
	private AjaxGlueXmlRpc getSessionGlueXmlRpc(HttpServletRequest request) throws HttpException {
		EjbLink ejb = getLoggedInEjb(request);

		return ejb.getEjb(AjaxGlueXmlRpc.class);
	}

	private AjaxGlueHttp getSessionGlueHttp(HttpServletRequest request) throws HttpException {
		EjbLink ejb = getLoggedInEjb(request);

		return ejb.getEjb(AjaxGlueHttp.class);
	}
	
	private void logRequest(HttpServletRequest request, String type) {
		logger.debug(type + " uri=" + request.getRequestURI() + " content-type=" + request.getContentType());
		Enumeration names = request.getAttributeNames(); 
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			
			logger.debug("--attr " + name + " = " + request.getAttribute(name));
		}
		
		names = request.getParameterNames();		
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			String[] values = request.getParameterValues(name);
			StringBuilder builder = new StringBuilder();
			for (String v : values) {
				builder.append("'" + v + "',");
			}
			builder.deleteCharAt(builder.length() - 1); // drop comma
			
			logger.debug("--param " + name + " = " + builder.toString());
		}
	}
	
	private boolean tryCheckLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// special method that magically causes us to look at your cookie and log you 
		// in if it's set, then return person you're logged in as or "false"
		if (!request.getRequestURI().equals("/text/checklogin")) {
			return false;
		}
		
		logger.debug("sending checklogin reply");
		
		boolean loggedIn = true;
		EjbLink ejb = null;
		try {
			ejb = getLoggedInEjb(request);
			if (!ejb.isLoggedIn()) // I think it always throws in this case, but
				loggedIn = false;
		} catch (HttpException e) {
			// for this call it isn't an error if we aren't logged in ...
			logger.debug(e);
			loggedIn = false;
		}
			
		response.setContentType("text/plain");
		OutputStream out = response.getOutputStream();
		if (loggedIn)
			out.write(ejb.getLoggedInUser().getBytes());
		else
			out.write("false".getBytes());
		out.flush();
		
		return true;
	}
	
	/* In doPost/doGet if we throw ServletException it shows the user a backtrace. We can also do 
	 * UnavailableException which I think sends the "temporarily unavailable" status code 
	 */
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		logRequest(request, "POST");
		
		if (tryCheckLogin(request, response)) {
			return;
		} else if (request.getRequestURI().startsWith("/xml") || request.getRequestURI().startsWith("/text")) {
			AjaxGlueHttp glue;
		
			try {
				glue = getSessionGlueHttp(request);
			} catch (HttpException e) {
				logger.debug(e);
				e.send(response);
				return;
			}
			if (glue == null)
				throw new RuntimeException("Could not create EJB");
		
			try {
				invokeHttpRequest(glue, request, response);
			} catch (HttpException e) {
				e.send(response);
				return;
			}
		} else if (request.getRequestURI().startsWith("/xmlrpc/")) {		
			XmlRpcServer xmlrpc;
			try {
				xmlrpc = getSessionXmlRpc(request);
			} catch (HttpException e) {
				e.send(response);
				return;
			}

			// no idea if xmlrpc is in fact thread-safe, but 
			// let's serialize all our uses of it... it's per-session
			// so should be no thread contention kind of issues
			synchronized (xmlrpc) {
				byte[] result = xmlrpc.execute(request.getInputStream());
				response.setContentType("text/xml");
				response.setContentLength(result.length);
				OutputStream out = response.getOutputStream();
				out.write(result);
				out.flush();
			}
		} else {
			HttpResponseCode.NOT_FOUND.send(response, "no such uri: " + request.getRequestURI());
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logRequest(request, "GET");
		
		if (tryCheckLogin(request, response)) {
			return;
		}
		
		AjaxGlueHttp glue;
		
		try {
			glue = getSessionGlueHttp(request);
		} catch (HttpException e) {
			logger.debug(e);
			e.send(response);
			return;
		}
		if (glue == null)
			throw new RuntimeException("Could not create EJB");
		
		try {
			invokeHttpRequest(glue, request, response);
		} catch (HttpException e) {
			e.send(response);
		}
	}
}
