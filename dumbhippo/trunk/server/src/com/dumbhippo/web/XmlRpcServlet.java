package com.dumbhippo.web;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.xmlrpc.XmlRpcServer;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.HttpContentTypes;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpParams;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.XmlRpcMethods;
import com.dumbhippo.web.CookieAuthentication.NotLoggedInException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

public class XmlRpcServlet extends HttpServlet {

	private static final Log logger = GlobalSetup.getLog(XmlRpcServlet.class);
	
	private static final long serialVersionUID = 0L;
	
	private static final String XMLRPC_KEY = "org.dumbhippo.web.XmlRpcServlet.XmlRpcServer";

	enum HttpResponseCode {
		
		// There are a lot more response codes we aren't wrapping here
		
		OK(HttpServletResponse.SC_OK),
		
		// This means the form submitted OK but there's no reply data
		NO_CONTENT(HttpServletResponse.SC_NO_CONTENT),
		
		BAD_REQUEST(HttpServletResponse.SC_BAD_REQUEST),
		
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
			logger.debug("Sending HTTP response code " + this + ": " + message);
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
	
	private Object[] marshalHttpRequestParams(HttpServletRequest request, OutputStream out, HttpResponseData replyContentType, HttpParams paramsAnnotation, Method m) throws HttpException {
		Class<?> args[] = m.getParameterTypes();

		ArrayList<Object> toPassIn = new ArrayList<Object>();
		
		int i = 0;
				
		boolean methodCanReturnContent = args.length > i && OutputStream.class.isAssignableFrom(args[i]);
		
		if (methodCanReturnContent) {
			toPassIn.add(out);
			i += 1;
			if (!(args.length > i && HttpResponseData.class.isAssignableFrom(args[i]))) {
				throw new RuntimeException("HTTP method " + m.getName() + " must have HttpResponseData contentType as arg " + i);
			}
			toPassIn.add(replyContentType);
			i += 1;
		} else if (replyContentType != HttpResponseData.NONE) {
			throw new RuntimeException("HTTP method " + m.getName() + " must have OutputStream as arg " + i + " to return type " + replyContentType);
		}
		
		if (args.length > i && Person.class.isAssignableFrom(args[i])) {
			Person loggedIn = getLoggedInPerson(request);
			toPassIn.add(loggedIn);
			i += 1;
		}
		
		if (args.length != i + paramsAnnotation.value().length) {
			throw new RuntimeException("HTTP method " + m.getName() + " should have params " + paramsAnnotation.value());
		}
		
		for (String pname : paramsAnnotation.value()) {
			if(!String.class.isAssignableFrom(args[i]))
				throw new RuntimeException("Only args of type String supported for now, arg " + i + " is type " + args[i].getCanonicalName());
			
			Object o = request.getParameter(pname);
			
			if (o == null) {
				throw new HttpException(HttpResponseCode.BAD_REQUEST,
						"Parameter " + pname + " not provided to method " + m.getName());
			}
			
			toPassIn.add(o);
			
			++i;
		}
		
		return toPassIn.toArray();
	}
	
	private <T> void invokeHttpRequest(T object, HttpServletRequest request, HttpServletResponse response)
			throws IOException, HttpException {
		
		String requestUri = request.getRequestURI();
		Class<?>[] interfaces = object.getClass().getInterfaces();
		boolean isPost = request.getMethod().toUpperCase().equals("POST"); 
		String[] uriComponents = parseUri(requestUri);
		String typeDir = uriComponents[0];
		String requestedMethod = uriComponents[1];
		String getRequestedMethod = "get" + requestedMethod;
		String doRequestedMethod = null;
		if (isPost) {
			doRequestedMethod = "do" + requestedMethod;
			logger.debug("is a post, will also look for " + doRequestedMethod);
		}

		logger.debug("trying to invoke http request at " + requestUri);
		
		HttpResponseData requestedContentType;
		if (typeDir.equals("xml"))
			requestedContentType = HttpResponseData.XML;
		else if (typeDir.equals("text"))
			requestedContentType = HttpResponseData.TEXT;
		else if (isPost && typeDir.equals("action"))
			requestedContentType = HttpResponseData.NONE;
		else {
			throw new HttpException(HttpResponseCode.NOT_FOUND,
					"Don't know about URI path /" + typeDir + " , only /xml, /text for GET plus /action for POST only)");
		}

		for (Class<?> iface : interfaces) {
			logger.debug("Looking for method " + requestedMethod + " on " + iface.getCanonicalName());
			for (Method m : iface.getMethods()) {
				HttpContentTypes contentAnnotation = m.getAnnotation(HttpContentTypes.class);
				HttpParams paramsAnnotation = m.getAnnotation(HttpParams.class);

				if (contentAnnotation == null) {
					logger.debug("Method " + m.getName() + " has no content type annotation, skipping");
					continue;
				}

				String lowercase = m.getName().toLowerCase();
				if (!(lowercase.equals(getRequestedMethod) || lowercase.equals(doRequestedMethod))) {
					logger.debug("Method " + m.getName() + " does not match " + getRequestedMethod + " or "
							+ doRequestedMethod + ", skipping");
					continue;
				}

				logger.debug("found method " + m.getName());

				if (m.getReturnType() != void.class)
					throw new RuntimeException("HTTP method " + m.getName() + " must return void not "
							+ m.getReturnType().getCanonicalName());

				boolean requestedContentTypeSupported = false;
				for (HttpResponseData t : contentAnnotation.value()) {
					if (t == requestedContentType) {
						requestedContentTypeSupported = true;
						break;
					}
				}

				if (!requestedContentTypeSupported) {
					throw new HttpException(HttpResponseCode.NOT_FOUND, "Wrong content type requested "
							+ requestedContentType + " valid types for method are " + contentAnnotation.value());
				}

				OutputStream out = response.getOutputStream();
				Object[] methodArgs = marshalHttpRequestParams(request, out, requestedContentType, paramsAnnotation, m);
				

				if (requestedContentType != HttpResponseData.NONE)
					response.setContentType(requestedContentType.getMimeType());

				try {
					logger.debug("Invoking method " + m.getName() + " with args " + Arrays.toString(methodArgs));
					m.invoke(object, methodArgs);
				} catch (IllegalArgumentException e) {
					logger.error(e);
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					logger.error(e);
					throw new RuntimeException(e);
				} catch (InvocationTargetException e) {
					logger.debug("Exception thrown by invoked method: " + e.getCause());
					logger.error(e.getCause());
					throw new RuntimeException(e);
				}

				out.flush();

				logger.debug("Reply for " + m.getName() + " sent");
				
				break; // don't call two different methods!
			}
		}
	}
	
	private Person getLoggedInPerson(HttpServletRequest request) throws HttpException {
		try {
			return CookieAuthentication.authenticate(request);
		} catch (BadTastingException e) {
			// In an HTML servlet, we would redirect to a login page; but in this
			// servlet we can't do much, we have no UI

			logger.debug(e);
			throw new HttpException(HttpResponseCode.FORBIDDEN, "Authorization failed, please log in again (bad cookie)", e);
		} catch (NotLoggedInException e1) {
			// In an HTML servlet, we would redirect to a login page; but in this
			// servlet we can't do much, we have no UI
			logger.debug(e1);
			throw new HttpException(HttpResponseCode.FORBIDDEN, "You need to log in", e1);
		}
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
	
	private Person doLogin(HttpServletRequest request, HttpServletResponse response, boolean log) throws IOException, HttpException {
		Person user = null;
		HttpSession sess = request.getSession(false);
		// Right now this will only be created if a JSF page is visited
		if (sess != null) {
			SigninBean signin = SigninBean.getFromHttpSession(sess);
			if (signin != null) {
				logger.debug("retrieved signin from session");
				user = signin.getUser();
				if (user != null) {
					logger.info("cached authentication from session for: " + user);					
				}
			}
		} else {
			logger.debug("no http session available");			
		}
		if (user == null) {
			logger.debug("no user in session, trying cookie");					
			try {
				user = CookieAuthentication.authenticate(request);
				logger.info("successfully authenticated: " + user);
			} catch (BadTastingException e) {
				if (log)
					logger.error("failed to log in", e);
				user = null;
			} catch (NotLoggedInException e2) {
				if (log)
					logger.error("authentication failed", e2);
				user = null;
			}
		}
		return user;
	}
	
	private boolean tryLoginRequests(HttpServletRequest request, HttpServletResponse response) throws IOException, HttpException {
		// special method that magically causes us to look at your cookie and log you 
		// in if it's set, then return person you're logged in as or "false"
		// if you use /text/dologin instead of /text/checklogin, and are using
		// POST, then it will also add the account (insecure temporary test feature)
		
		boolean addAccount = request.getRequestURI().equals("/text/dologin") &&
					request.getMethod().toUpperCase().equals("POST");
		
		if (!addAccount && !request.getRequestURI().equals("/text/checklogin")) {
			return false;
		}
		
		Person user;	
		if (addAccount) {
			logger.debug("Adding account");
			
			String email = request.getParameter("email");
			if (email == null) {
				throw new HttpException(HttpResponseCode.BAD_REQUEST, "No email address provided");
			}

			LoginCookie loginCookie = AddClientBean.addNewClientForEmail(email, request, response);
			
			try {
				user = CookieAuthentication.authenticate(loginCookie);
			} catch (BadTastingException e) {
				logger.error("Cookie we just added failed to log in ", e);
				throw new HttpException(HttpResponseCode.INTERNAL_SERVER_ERROR,
						"Login failed because our system did not work correctly. Apologies.");
			} catch (NotLoggedInException e) {
				logger.error("Cookie we just added failed to log in ", e);
				throw new HttpException(HttpResponseCode.INTERNAL_SERVER_ERROR,
						"Login failed because our system did not work correctly. Apologies.");
			}
			
			logger.debug("account added");
		} else {
			user = doLogin(request, response, false);
		}
		
		logger.debug("sending checklogin reply");
			
		response.setContentType("text/plain");
		OutputStream out = response.getOutputStream();
		if (user != null)
			out.write(user.toString().getBytes());
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
		
		try {
			if (tryLoginRequests(request, response)) {
				return;
			} else if (request.getRequestURI().startsWith("/xmlrpc/")) {		
				XmlRpcServer xmlrpc = new XmlRpcServer();
				// Java thread locks are recursive so this is OK...
				XmlRpcMethods glue = WebEJBUtil.defaultLookup(XmlRpcMethods.class);
				
				// glue is a proxy that only exports the one interface, 
				// so safe to export it all
				xmlrpc.addHandler("dumbhippo", glue);				
				byte[] result = xmlrpc.execute(request.getInputStream());
				response.setContentType("text/xml");
				response.setContentLength(result.length);
				OutputStream out = response.getOutputStream();
				out.write(result);
				out.flush();
			} else {
				HttpMethods glue = WebEJBUtil.defaultLookup(HttpMethods.class);
			
				invokeHttpRequest(glue, request, response);
			}
		} catch (HttpException e) {
			logger.debug(e);
			e.send(response);
			return;
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logRequest(request, "GET");
		
		try {
			HttpMethods glue = WebEJBUtil.defaultLookup(HttpMethods.class);
			invokeHttpRequest(glue, request, response);
		} catch (HttpException e) {
			logger.debug(e);
			e.send(response);
			return;
		}
	}
}
