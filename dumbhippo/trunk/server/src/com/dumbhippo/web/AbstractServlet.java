package com.dumbhippo.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.HumanVisibleException;

public abstract class AbstractServlet extends HttpServlet {

	private static final Logger logger = GlobalSetup.getLogger(AbstractServlet.class);
	
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

	protected void forwardToErrorPage(HttpServletRequest request, HttpServletResponse response, HumanVisibleException e)
		throws ServletException, IOException {
		request.setAttribute("errorHtml", e.getHtmlMessage());
		if (e.getHtmlSuggestion() != null)
			request.setAttribute("suggestionHtml", e.getHtmlSuggestion());
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
	
	protected User doLogin(HttpServletRequest request) throws IOException, HttpException {
		return SigninBean.getForRequest(request).getUser();
	}

	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
		  	HumanVisibleException, IOException, ServletException {
		throw new HttpException(HttpResponseCode.NOT_FOUND, "POST not implemented");				 
	}

	protected void wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			HumanVisibleException, IOException, ServletException {
		throw new HttpException(HttpResponseCode.NOT_FOUND, "GET not implemented");				 
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
	
	void sendFile(HttpServletRequest request, HttpServletResponse response, String contentType, File file) throws IOException {
		logger.debug("sending file " + file.getCanonicalPath());
		response.setContentType(contentType);
		InputStream in = new FileInputStream(file);
		OutputStream out = response.getOutputStream();
		copy(in, out);
		out.flush();		
	}

	/** 
	 * Set headers on a response indicating that the response 
	 * data should not be cached.
	 * @param response the response object
	 */
	public static void setNoCache(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma", "no-cache");
	}
	
	protected static final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
	protected final static TimeZone gmtZone = TimeZone.getTimeZone("GMT");

	static {
		format.setTimeZone(gmtZone);
	}
	
	/**
	 * Set headers on a response indicating that the response data never expires.
	 * This is typically used in conjunction for a resource pointed to with a 
	 * versioned URL.
	 * 
	 * @param response the response object 
	 */
	public static void setInfiniteExpires(HttpServletResponse response) {
		// According to to HTTP spec we shouldn't set a date more than 1 year in advance
		String expires = format.format(new Date(System.currentTimeMillis() + (364 * 24 * 60 * 60 * 1000l)));
		response.setHeader("Expires", expires);	
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
			logger.debug("http exception processing POST", e);
			e.send(response);
		} catch (HumanVisibleException e) {
			logger.debug("human visible exception", e);
			forwardToErrorPage(request, response, e);
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logRequest(request, "GET");
		
		try {
			wrappedDoGet(request, response);
		} catch (HttpException e) {
			logger.debug("http exception processing GET", e);
			e.send(response);
		} catch (HumanVisibleException e) {
			logger.debug("human visible exception", e);
			forwardToErrorPage(request, response, e);
		}
	}
}
