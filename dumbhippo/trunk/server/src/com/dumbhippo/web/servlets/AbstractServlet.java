package com.dumbhippo.web.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;

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
			logger.debug("Sending HTTP response code {}: {}", this, message);
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
	
	protected String redirectToNextPage(HttpServletRequest request, HttpServletResponse response, String next, String flashMessage)
		throws ServletException, IOException {
		// if we have a flash message or need to close the window, we have to load a special
		// page that does that in JavaScript; otherwise we can just redirect you straightaway
		if (flashMessage != null || next.equals("/close") || next.equals("close")) {
			request.setAttribute("next", next);
			if (flashMessage != null)
				request.setAttribute("flashMessage", flashMessage);
			return "/flash";
		} else {
			String url = response.encodeRedirectURL(next);
			response.sendRedirect(url);
			
			return null;
		}
	}
	
	protected void logRequest(HttpServletRequest request, String type) {
		logRequest(request, type, false); // FIXME false for requires transaction is misleading
	}
	
	private void logRequest(HttpServletRequest request, String type, boolean requiresTransaction) {
		if (!logger.isDebugEnabled()) // avoid this expense entirely in production
			return;
		
		// this line of debug is cut-and-pasted over to RewriteServlet also
		logger.debug("--------------- HTTP {} for '{}' content-type='" +
				request.getContentType() + "' transaction=" + requiresTransaction,
				type, request.getRequestURI());
		
		Enumeration names = request.getAttributeNames(); 
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			
			logger.debug("request attr {} = {}", name, request.getAttribute(name));
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
			// suppress plaintext password from appearing in log
			String showValue = builder.toString();
			if (name.equals("password")) 
				showValue = "[SUPPRESSED FROM LOG]";
			logger.debug("param {} = {}", name, showValue);
		}
	}
	
	protected Viewpoint getViewpoint(HttpServletRequest request) {
		SigninBean signin = SigninBean.getForRequest(request);
		return signin.getViewpoint();
	}
	
	protected User getUser(HttpServletRequest request) {
		SigninBean signin = SigninBean.getForRequest(request);
		if (signin.isValid())
			return ((UserSigninBean)signin).getUser();
		else 
			return null;
	}
	
	protected User doLogin(HttpServletRequest request) throws IOException, HttpException {
		User user = getUser(request);
		if (user != null)
			return user;
		else 
			throw new HttpException(HttpResponseCode.FORBIDDEN, "You must be logged in");
	}

	/**
	 * Override with the actual implementation of the POST request; if a non-NULL string
	 * is returned, will forward the request to the (relative) URL represented by the
	 * string. This replaces redirect use of request.getRequestDispatcher(<url>).forward()
	 * because the the forward must be executed outside of the transaction that we may
	 * add around wrappedDoPost(). If you want a redirect, just call request.sendRedirect(),
	 * since there is no issue with transactions in that case ... the client will make
	 * a new request.
	 */
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
		  	HumanVisibleException, IOException, ServletException {
		throw new HttpException(HttpResponseCode.NOT_FOUND, "POST not implemented");				 
	}

	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			HumanVisibleException, IOException, ServletException {
		throw new HttpException(HttpResponseCode.NOT_FOUND, "GET not implemented");				 
	}
		
	void sendFile(HttpServletRequest request, HttpServletResponse response, String contentType, File file) throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("sending file {}", file.getCanonicalPath());
		response.setContentType(contentType);
		InputStream in = new FileInputStream(file);
		OutputStream out = response.getOutputStream();
		StreamUtils.copy(in, out);
		out.flush();
		in.close();
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
	
	/**
	 * Set UTF-8 as the request encoding for the request if no other encoding
	 * is set. The request encoding determines the interpretation of request
	 * parameters, among other things.
	 * @param request the request object
	 */
	public static void ensureRequestEncoding(HttpServletRequest request) {
		if (request.getCharacterEncoding() == null) {
			try {
				request.setCharacterEncoding("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Encoding UTF-8 is not supported");
			}
		}
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
	
	protected abstract boolean requiresTransaction(HttpServletRequest request);
	
	private Object runWithTransaction(HttpServletRequest request, Callable func) throws HumanVisibleException, HttpException {
		long startTime = System.currentTimeMillis();
		UserTransaction tx;
		try {
			InitialContext initialContext = new InitialContext();
			tx = (UserTransaction)initialContext.lookup("UserTransaction");
		} catch (NamingException e) {
			throw new RuntimeException("Cannot create UserTransaction");
		}
		
		try {
			tx.begin();
		} catch (NotSupportedException e) {
			throw new RuntimeException("Error starting transaction", e); 
		} catch (SystemException e) {
			throw new RuntimeException("Error starting transaction", e); 
		}
		try {
			Object result = func.call();
			logger.debug("Handled {} in {} milliseconds", request.getPathInfo(), System.currentTimeMillis() - startTime);
			return result;
		} catch (Throwable t) {
			try {
				// We don't try to duplicate the complicated EJB logic for whether
				// the transaction should be rolled back; we just roll it back
				// always if an exception occurs. After all, the transaction
				// probably didn't do any writing to start with.
				tx.setRollbackOnly();
			} catch (SystemException e) {
				throw new RuntimeException("Error setting transaction for rollback");
			}
			
			if (t instanceof Error)
				throw (Error)t;
			else if (t instanceof HumanVisibleException)
				throw (HumanVisibleException)t;
			else if (t instanceof HttpException)
				throw (HttpException)t;
			else if (t instanceof RuntimeException)
				throw (RuntimeException)t;
			else
				throw new RuntimeException(t);
		} finally {
			try {
				if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK)
					tx.rollback();
				else
					tx.commit();
			} catch (SystemException e) {
				throw new RuntimeException("Error ending transaction", e);
			} catch (RollbackException e) {
				throw new RuntimeException("Error ending transaction", e);
			} catch (HeuristicMixedException e) {
				throw new RuntimeException("Error ending transaction", e);
			} catch (HeuristicRollbackException e) {
				throw new RuntimeException("Error ending transaction", e);
			}
		}						
	}
	
	private Object runWithErrorPage(HttpServletRequest request, HttpServletResponse response, Callable func) throws IOException, ServletException {
		try {
			try {
				return func.call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (RuntimeException e) {
			Throwable cause = e.getCause();
			if (cause instanceof HttpException) {
				logger.debug("http exception processing " + request.getMethod(), e);
				((HttpException) cause).send(response);
				return null;
			} else if (cause instanceof HumanVisibleException) {
				logger.debug("human visible exception: {}", e.getMessage());
				forwardToErrorPage(request, response, (HumanVisibleException) cause);
				return null;
			} else {
				throw e;
			}
		}
	}
	
	// Instead of just forwarding http requests to the right handler, we surround
	// them in a transaction if requested. The point is to get the entire request 
	// to use a single Hibernate session. This improves performance, since
	// persistance beans are cached across the session, and also means
	// that persistance beans returned to the web tier won't be detached.
	//
	// While we are add it, we time the request for performancing monitoring	
	private Object runWithTransactionAndErrorPage(final HttpServletRequest request, final HttpServletResponse response, final Callable func) throws IOException, ServletException  {
		return runWithErrorPage(request, response, new Callable() {
			public Object call() throws Exception {
				return runWithTransaction(request, func);
			}
		});
	}
	
	/*
	 * In doPost/doGet if we throw ServletException it shows the user a
	 * backtrace. We can also do UnavailableException which I think sends the
	 * "temporarily unavailable" status code
	 */
	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException {
		ensureRequestEncoding(request);
		boolean requiresTransaction = requiresTransaction(request);
		logRequest(request, "POST", requiresTransaction);
		String forwardUrl;
		if (requiresTransaction) {
			 forwardUrl = (String)runWithTransactionAndErrorPage(request, response, new Callable() { 
				public Object call() throws Exception {
					return wrappedDoPost(request, response);
				}
			});
		} else {
			forwardUrl = (String)runWithErrorPage(request, response, new Callable() {
				public Object call() throws Exception {
					return wrappedDoPost(request, response);
				}
			});
		}
		
		if (forwardUrl != null)
			request.getRequestDispatcher(forwardUrl).forward(request, response);
	}
	
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		ensureRequestEncoding(request);
		boolean requiresTransaction = requiresTransaction(request); 
		logRequest(request, "GET", requiresTransaction);
		String forwardUrl;
		if (requiresTransaction) {
			forwardUrl = (String)runWithTransactionAndErrorPage(request, response, new Callable() { 
				public Object call() throws Exception {
					return wrappedDoGet(request, response);
				}
			});
		} else {
			forwardUrl = (String)runWithErrorPage(request, response, new Callable() {
				public Object call() throws Exception {
					return wrappedDoGet(request, response);
				}
			});
		}		

		if (forwardUrl != null)
			request.getRequestDispatcher(forwardUrl).forward(request, response);
	}
}
