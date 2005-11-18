package com.dumbhippo.web;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.User;

public abstract class AbstractServlet extends HttpServlet {

	private static final Log logger = GlobalSetup.getLog(AbstractServlet.class);
	
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
	
	static class HttpException extends Exception {
		
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
	
	/**
	 * This exception will be shown to the user on an error page.
	 * Don't put technobabble in the message.
	 */
	static class ErrorPageException extends Exception {
		private static final long serialVersionUID = 0L;
		
		ErrorPageException(String message) {
			super(message);
		}
	}

	protected void forwardToErrorPage(HttpServletRequest request, HttpServletResponse response, String errorText)
		throws ServletException, IOException {
		request.setAttribute("errorText", errorText);
		request.getRequestDispatcher("/error").forward(request, response);
	}
	
	protected void redirectToNextPage(HttpServletRequest request, HttpServletResponse response, String next, String flashMessage)
		throws ServletException, IOException {
		// if we have a flash message or need to close the window, we have to load a special
		// page that does that in JavaScript; otherwise we can just redirect you straightaway
		if (flashMessage != null || next.equals("/close") || next.equals("close")) {
			request.setAttribute("next", next);
			if (flashMessage != null)
				request.setAttribute("flashMessage", flashMessage);
			request.getRequestDispatcher("/flash").forward(request, response);
		} else {
			String url = response.encodeRedirectURL(next);
			response.sendRedirect(url);
		}
	}
	
	protected void logRequest(HttpServletRequest request, String type) {
		logger.debug(type + " uri=" + request.getRequestURI() + " content-type=" + request.getContentType());
		Enumeration names = request.getAttributeNames(); 
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			
			logger.debug("request attr " + name + " = " + request.getAttribute(name));
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
			
			logger.debug("param " + name + " = " + builder.toString());
		}
	}
	
	protected User doLogin(HttpServletRequest request, HttpServletResponse response, boolean log) throws IOException, HttpException {
		// FIXME what is the "log" parameter about?
		return SigninBean.getForRequest(request).getUser();
	}	

	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
		  	ErrorPageException, IOException, ServletException {
		throw new HttpException(HttpResponseCode.NOT_FOUND, "POST not implemented");				 
	}

	protected void wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			ErrorPageException, IOException, ServletException {
		throw new HttpException(HttpResponseCode.NOT_FOUND, "GET not implemented");				 
	}

	/*
	 * In doPost/doGet if we throw ServletException it shows the user a
	 * backtrace. We can also do UnavailableException which I think sends the
	 * "temporarily unavailable" status code
	 */
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		logRequest(request, "POST");
		
		try {
			wrappedDoPost(request, response);
		} catch (HttpException e) {
			logger.debug(e);
			e.send(response);
		} catch (ErrorPageException e) {
			logger.debug(e);
			forwardToErrorPage(request, response, e.getMessage());
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logRequest(request, "GET");
		
		try {
			wrappedDoGet(request, response);
		} catch (HttpException e) {
			logger.debug(e);
			e.send(response);
		} catch (ErrorPageException e) {
			logger.debug(e);
			forwardToErrorPage(request, response, e.getMessage());
		}
	}
}
