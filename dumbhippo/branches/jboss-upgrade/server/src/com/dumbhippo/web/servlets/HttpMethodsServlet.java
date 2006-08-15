package com.dumbhippo.web.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.HttpContentTypes;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpOptions;
import com.dumbhippo.server.HttpParams;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.web.DisabledSigninBean;
import com.dumbhippo.web.LoginCookie;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;
import com.dumbhippo.web.WebEJBUtil;
import com.dumbhippo.web.WebStatistics;

public class HttpMethodsServlet extends AbstractServlet {

	private static final Logger logger = GlobalSetup.getLogger(HttpMethodsServlet.class);
	
	private static final long serialVersionUID = 0L;
	
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
	
	private Object[] marshalHttpRequestParams(HttpServletRequest request, Object out,
			HttpResponseData replyContentType, HttpParams paramsAnnotation, HttpOptions optionsAnnotation,
			Method m) throws HttpException {
		Class<?> args[] = m.getParameterTypes();

		ArrayList<Object> toPassIn = new ArrayList<Object>();
		
		int i = 0;
				 
		// FIXME only allow XmlBuilder for XMLMETHOD
		boolean methodCanReturnContent = args.length > i 
						&& (OutputStream.class.isAssignableFrom(args[i])
							|| XmlBuilder.class.isAssignableFrom(args[i]));
		
		if (methodCanReturnContent) {
			toPassIn.add(out);
			i += 1;
			if (replyContentType != HttpResponseData.XMLMETHOD) {
				if (!(args.length > i && HttpResponseData.class.isAssignableFrom(args[i]))) {
					throw new RuntimeException("HTTP method " + m.getName() + " must have HttpResponseData contentType as arg " + i);
				}
				toPassIn.add(replyContentType);
				i += 1;
			}
		} else if (replyContentType != HttpResponseData.NONE) {
			throw new RuntimeException("HTTP method " + m.getName() + " must have OutputStream or Writer as arg " + i + " to return type " + replyContentType);
		}
		
		if (args.length > i && UserViewpoint.class.isAssignableFrom(args[i])) {
			boolean allowDisabled = optionsAnnotation != null && optionsAnnotation.allowDisabledAccount();
			UserViewpoint loggedIn = getLoggedInUser(request, allowDisabled);
			boolean adminOnly = optionsAnnotation != null && optionsAnnotation.adminOnly();
			
			if (adminOnly) {
				IdentitySpider spider = EJBUtil.defaultLookup(IdentitySpider.class);
				if (!spider.isAdministrator(loggedIn.getViewer()))
						throw new HttpException(HttpResponseCode.FORBIDDEN, "Method requires admin authorization");				
			}
			toPassIn.add(loggedIn);
			i += 1;
		}
		
		if (args.length != i + paramsAnnotation.value().length) {
			throw new RuntimeException("HTTP method " + m.getName() + " should have params " + Arrays.toString(paramsAnnotation.value()));
		}
		
		for (String pname : paramsAnnotation.value()) {
			String s = request.getParameter(pname);
			
			if (s == null) {
				boolean isOptional = false;
				if (optionsAnnotation != null) {
					String[] optional = optionsAnnotation.optionalParams();
					for (String o : optional) {
						if (o.equals(pname)) {
							isOptional = true;
							break;
						}
					}
				}

				if (!isOptional)
					throw new HttpException(HttpResponseCode.BAD_REQUEST,
							"Parameter " + pname + " not provided to method " + m.getName());
			}
			
			if(String.class.isAssignableFrom(args[i])) {
				toPassIn.add(s);
			} else if (boolean.class.isAssignableFrom(args[i])) {
				if (s == null) {
					toPassIn.add(false); // default to false on optional bool args
				} else if (s.equals("true")) {
					toPassIn.add(true);
				} else if (s.equals("false")) {
					toPassIn.add(false);
				} else {
					throw new HttpException(HttpResponseCode.BAD_REQUEST, "Parameter " + pname + " to method " + m.getName() + " must be 'true' or 'false'");
				}
			} else {
				throw new RuntimeException("Arg " + i + " of type " + args[i].getCanonicalName() + " is not supported");
			}
			
			++i;
		}
		
		return toPassIn.toArray();
	}
	
	private void writeXmlMethodError(OutputStream out, String code, String message) throws IOException {
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement("rsp", "stat", "fail");
		xml.appendTextNode("err", "", "code", code, "msg", message);
		xml.closeElement();
		out.write(StringUtils.getBytes(xml.toString()));
	}	
	
	private void writeXmlMethodError(OutputStream out, String message) throws IOException {
		writeXmlMethodError(out, "red", message);
	}
	
	private String arrayToStringXmlBuilderWorkaround(Object[] array) {
		// XmlBuilder.toString() has kind of a broken side effect of closing the XML document, 
		// so we can't use Arrays.toString()
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (Object arg : array) {
			if (arg instanceof XmlBuilder)
				sb.append("XmlBuilder");
			else
				sb.append(arg != null ? arg.toString() : "null");
			sb.append(", ");
		}
		if (array.length > 0)
			sb.setLength(sb.length() - 2); // delete last comma
		sb.append("}");
		return sb.toString();
	}
	
	private <T> void invokeHttpRequest(T object, HttpServletRequest request, HttpServletResponse response)
			throws IOException, HttpException, HumanVisibleException {
		
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
			//logger.debug("is a POST, will also look for {}", doRequestedMethod);
		}

		//logger.debug("trying to invoke http request at {}", requestUri);
		
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

		boolean foundMethod = false;
		
		for (Class<?> iface : interfaces) {
			
			logger.debug("Looking for method {} on {}", requestedMethod, iface.getCanonicalName());
			for (Method m : iface.getMethods()) {
				HttpContentTypes contentAnnotation = m.getAnnotation(HttpContentTypes.class);
				HttpParams paramsAnnotation = m.getAnnotation(HttpParams.class);
				HttpOptions optionsAnnotation = m.getAnnotation(HttpOptions.class);

				if (contentAnnotation == null) {
					logger.debug("Method {} has no content type annotation, skipping", m.getName());
					continue;
				}

				if (paramsAnnotation == null) {
					throw new HttpException(HttpResponseCode.INTERNAL_SERVER_ERROR, "missing params annotation on " + m.getName());
				}
				
				String lowercase = m.getName().toLowerCase();
				if (!(lowercase.equals(getRequestedMethod) || lowercase.equals(doRequestedMethod))) {
					/*
					logger.debug("Method " + m.getName() + " does not match " + getRequestedMethod + " or "
							+ doRequestedMethod + ", skipping");
					*/
					continue;
				}

				//logger.debug("found method " + m.getName());

				if (m.getReturnType() != void.class)
					throw new RuntimeException("HTTP method " + m.getName() + " must return void not "
							+ m.getReturnType().getCanonicalName());

				boolean requestedContentTypeSupported = false;
				for (HttpResponseData t : contentAnnotation.value()) {				
					if (t.equals(HttpResponseData.XMLMETHOD)) {
						requestedContentTypeSupported = true;
						requestedContentType = HttpResponseData.XMLMETHOD;
						break;
					}
				}
				if (!requestedContentTypeSupported) {
					for (HttpResponseData t : contentAnnotation.value()) {
						// For XMLMETHOD methods, we skip this check versus the
						// default requested content type of NONE
						if (t == requestedContentType) {
							requestedContentTypeSupported = true;
							break;
						}
					}
				}

				if (!requestedContentTypeSupported) {
					throw new HttpException(HttpResponseCode.NOT_FOUND, "Wrong content type requested "
							+ requestedContentType + " valid types for method are " + contentAnnotation.value());
				}

				Object out;
				if (requestedContentType.equals(HttpResponseData.XMLMETHOD)) {
					XmlBuilder xml = new XmlBuilder();
					out = xml;
					xml.appendStandaloneFragmentHeader();
					xml.openElement("rsp", "stat", "ok");
				} else {
					out = response.getOutputStream();
				}
				Object[] methodArgs = marshalHttpRequestParams(request, out, requestedContentType,
						paramsAnnotation, optionsAnnotation, m);

				if (requestedContentType != HttpResponseData.NONE)
					response.setContentType(requestedContentType.getMimeType());

				boolean trappedError = false;
				try {
					if (logger.isDebugEnabled()) {						
						String showArgs = arrayToStringXmlBuilderWorkaround(methodArgs);
						// suppress plaintext password from appearing in log
						if ((m.getName() != null) && (m.getName().equals("doSetPassword")))
							showArgs = "[SUPPRESSED FROM LOG]";
						logger.debug("Invoking method {} with args {}", m.getName(), showArgs);
					}
					m.invoke(object, methodArgs);
				} catch (IllegalArgumentException e) {
					logger.error("invoking method on http methods bean {}", e.getMessage());
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					logger.error("invoking method on http methods bean {}", e.getMessage());
					throw new RuntimeException(e);
				} catch (InvocationTargetException e) {
					Throwable cause = e.getCause();
					Throwable rootCause = ExceptionUtils.getRootCause(e);
					logger.error("Exception root cause " + rootCause.getClass().getName() + " thrown by invoked method: " + rootCause.getMessage(), rootCause);
					
					WebStatistics.getInstance().incrementHttpMethodErrors();

					if (cause instanceof HumanVisibleException) {
						HumanVisibleException visibleException = (HumanVisibleException) cause;
						if (requestedContentType.equals(HttpResponseData.XMLMETHOD)) {
							writeXmlMethodError(response.getOutputStream(), visibleException.getMessage());
							trappedError = true;
						} else {
							throw visibleException;
						}
					} else if (cause instanceof XmlMethodException){
						XmlMethodException methodException = (XmlMethodException) cause;
						if (requestedContentType.equals(HttpResponseData.XMLMETHOD)) {
							writeXmlMethodError(response.getOutputStream(), methodException.getCodeString(), methodException.getMessage());
							trappedError = true;
						} else {
							throw new RuntimeException("Non-XMLMETHOD threw an XmlMethodException", methodException);
						}
					} else {
						throw new RuntimeException(e);
					}
				}
				
				if (!trappedError)
					WebStatistics.getInstance().incrementHttpMethodsServed();
				
				// The point of the allowDisabledUser annotations is to allow methods that
				// take a user from the disabled state to the enabled state; once we are
				// enabled, we need to persistant cookies, so we check that here. This
				// is a little hacky, but simpler than creating custom servlets.
				//
				// Note that this won't work well with methods that have write
				// output, since the output buffer may already have been flushed, and it
				// will be too late to set cookies.
				if (optionsAnnotation != null && optionsAnnotation.allowDisabledAccount()) {
					SigninBean.updateAuthentication(request, response);
				}

				
				if (optionsAnnotation != null && optionsAnnotation.invalidatesSession()) {
					HttpSession sess = request.getSession(false);
					if (sess != null)
						sess.invalidate();	
				}

				if (requestedContentType.equals(HttpResponseData.XMLMETHOD) && !trappedError) {
					XmlBuilder xml = (XmlBuilder) out;
					xml.closeElement();
					response.getOutputStream().write(xml.getBytes());
				}
				
				response.getOutputStream().flush();

				logger.debug("Reply for {} sent", m.getName());
		
				foundMethod = true;
				break; // stop scanning this interface
			}
			
			if (foundMethod)
				break; // also stop scanning this object
		}
		
		if (!foundMethod) {
			throw new HttpException(HttpResponseCode.NOT_FOUND, "No such method " + requestedMethod);
		}
	}
	
	private UserViewpoint getLoggedInUser(HttpServletRequest request, boolean allowDisabled) throws HttpException {
		SigninBean signin = SigninBean.getForRequest(request);
		if (signin instanceof UserSigninBean) {
			return ((UserSigninBean)signin).getViewpoint();
		} else if (allowDisabled && signin instanceof DisabledSigninBean) {
			return new UserViewpoint(((DisabledSigninBean)signin).getDisabledUser());
		}

		// we have no UI so the user is pretty much jacked at this stage; but it 
		// should not happen in any non-broken situation
		throw new HttpException(HttpResponseCode.FORBIDDEN, "You need to log in");
	}
	
	private boolean tryLoginRequests(HttpServletRequest request, HttpServletResponse response) throws IOException, HttpException {
		// special method that magically causes us to look at your cookie and log you 
		// in if it's set, then return person you're logged in as or "false"
		
		if (!(request.getRequestURI().equals("/text/dologin") &&
				request.getMethod().toUpperCase().equals("POST"))
				&& !request.getRequestURI().equals("/text/checklogin")) {
			return false;
		}
			
		User user = getUser(request);
			
		response.setContentType("text/plain");
		OutputStream out = response.getOutputStream();
		if (user != null)
			out.write(user.getId().getBytes());
		else
			out.write("false".getBytes());
		out.flush();
		
		return true;
	}

	private boolean trySignoutRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, HttpException { 
		if (!request.getRequestURI().equals("/action/signout") ||
		    !request.getMethod().toUpperCase().equals("POST"))
			return false;
		
		HttpSession session = request.getSession();
		if (session != null)
			session.invalidate();		
		
		// FIXME we need to drop the Client object when we do this,
		// both to save our own disk space, and in case someone stole the 
		// cookie.
		
		response.addCookie(LoginCookie.newDeleteCookie());
		
		return true;
	}
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException, HumanVisibleException {
		
		setNoCache(response);
		
		if (tryLoginRequests(request, response) ||
		    trySignoutRequest(request, response)) {
			/* nothing */
		} else {
			HttpMethods glue = WebEJBUtil.defaultLookup(HttpMethods.class);

			invokeHttpRequest(glue, request, response);
		}
		
		return null;
	}
	
	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException, HumanVisibleException {
		setNoCache(response);
		
		HttpMethods glue = WebEJBUtil.defaultLookup(HttpMethods.class);
		invokeHttpRequest(glue, request, response);
		
		return null;
	}

	@Override
	protected boolean requiresTransaction() {
		return true;
	}
}
