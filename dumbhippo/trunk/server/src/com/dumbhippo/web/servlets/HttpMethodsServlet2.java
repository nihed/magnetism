package com.dumbhippo.web.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.server.HttpContentTypes;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpOptions;
import com.dumbhippo.server.HttpParams;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.XmlMethodErrorCode;
import com.dumbhippo.server.XmlMethodException;

/** 
 * Redoing HttpMethodsServlet. For now, the point of this is simply to 
 * provide a way to get docs on the web API. But eventually should move
 * the method invocation over to use this "precompiled" approach.
 * 
 * Other stuff to clean up:
 *  - have the urls be studlyCaps instead of all lowercased
 *  - orthogonalize content type vs. xml calling convention
 * 
 * @author Havoc Pennington
 *
 */
public class HttpMethodsServlet2 extends AbstractServlet {
	private static final Logger logger = GlobalSetup.getLogger(HttpMethodsServlet2.class);
	
	private static final long serialVersionUID = 0L;

	private static HttpMethodRepository repository;
	
	private static synchronized HttpMethodRepository getRepository() {
		if (repository == null) {
			repository = new HttpMethodRepository();
			repository.scanInterface(HttpMethods.class);
			repository.lock();
		}
		return repository;
	}
	
	private static class HttpMethodRepository {
		private Map<Class<?>,Marshaller> marshallers;
		
		private Map<String,HttpMethod> methods;

		private Map<String,HttpMethod> lowercaseMethods;
		
		private List<HttpMethod> sortedMethods;
		
		HttpMethodRepository() {
			methods = new HashMap<String,HttpMethod>();
			
			marshallers = new HashMap<Class<?>,Marshaller>();
			
			marshallers.put(String.class, new Marshaller() {
	
				public Object marshal(String s) throws XmlMethodException {
					return s;
				}
				
				public Class<?> getType() {
					return String.class;
				}
				
			});
			
			marshallers.put(boolean.class, new Marshaller() {
	
				public Object marshal(String s) throws XmlMethodException {
					if (s == null)
						return false;
					else if (s.equals("true"))
						return true;
					else if (s.equals("false"))
						return false;
					else
						throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "could not parse boolean value: '" + s + "'");
				}

				public Class<?> getType() {
					return boolean.class;
				}
				
			});		
		}
		
		Marshaller lookupMarshaller(Class<?> klass) {
			return marshallers.get(klass);
		}
		
		void scanInterface(Class<?> iface) {
			if (!iface.isInterface())
				throw new IllegalArgumentException("not an interface: " + iface.getName());
			int count = 0;
			logger.debug("Scanning interface {} for http-invokable methods", iface.getName());
			for (Method m : iface.getMethods()) {
				HttpContentTypes contentAnnotation = m.getAnnotation(HttpContentTypes.class);

				if (contentAnnotation == null) {
					logger.debug("  method {} has no content type annotation, skipping", m.getName());
					continue;
				}

				HttpMethod hMethod = new HttpMethod(m, contentAnnotation);
				
				methods.put(hMethod.getName(), hMethod);
				count += 1;
			}
			logger.debug("Found {} methods on {}", count, iface.getName());
		}
		
		void scanClass(Class<?> klass) {
			Class<?>[] interfaces = klass.getInterfaces();
			for (Class<?> iface : interfaces) {
				scanInterface(iface);
			}
		}
		
		Collection<HttpMethod> getMethods() {
			return sortedMethods;
		}
		
		HttpMethod lookupMethod(String name) {
			HttpMethod m = methods.get(name);
			if (m == null)
				m = lowercaseMethods.get(name);
			return m;
		}
		
		void lock() {
			if (sortedMethods != null)
				throw new IllegalStateException("can only lock() once");
			
			sortedMethods = new ArrayList<HttpMethod>();
			for (HttpMethod m : methods.values()) {
				sortedMethods.add(m);
			}
			Collections.sort(sortedMethods, new Comparator<HttpMethod>() {

				public int compare(HttpMethod a, HttpMethod b) {
					return String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName());
				}
			});
		
			// so we can return it by reference
			sortedMethods = Collections.unmodifiableList(sortedMethods);
			
			lowercaseMethods = new HashMap<String,HttpMethod>();
			for (HttpMethod m : methods.values()) {
				lowercaseMethods.put(m.getName().toLowerCase(), m);
			}
		}
	}
	
	private interface Marshaller {
		/**
		 * Marshal from a string parameter to a Java method argument
		 * 
		 * @param s string from http request, or null if param not provided
		 * @return the marshaled object (null is allowed)
		 * @throws XmlMethodException thrown if the string can't be parsed
		 */
		public Object marshal(String s) throws XmlMethodException;
		
		public Class<?> getType();
	}
	
	private static class HttpMethodParam {
		private String name;
		private boolean isOptional;
		private Marshaller marshaller;
		
		public HttpMethodParam(String name, boolean isOptional,
				Marshaller marshaller) {
			this.name = name;
			this.isOptional = isOptional;
			this.marshaller = marshaller;
		}

		public boolean isOptional() {
			return isOptional;
		}
		
		public String getName() {
			return name;
		}
		
		public Marshaller getMarshaller() {
			return marshaller;
		}
	}
	
	private static class HttpMethod {
		
		private String name;
		private boolean requiresPost;
		private List<HttpMethodParam> params;
		private boolean needsOutputStream;
		private boolean needsXmlBuilder;
		private boolean needsRequestedContentType;
		private boolean needsViewpoint;
		private boolean needsUserViewpoint;
		private Set<HttpResponseData> contentTypes;
		private boolean adminOnly;
		private boolean allowsDisabledAccount;
		
		HttpMethod(Method m,
				   HttpContentTypes contentAnnotation) {
			HttpParams paramsAnnotation = m.getAnnotation(HttpParams.class);
			HttpOptions optionsAnnotation = m.getAnnotation(HttpOptions.class);
			
			if (paramsAnnotation == null) {
				throw new RuntimeException("missing params annotation on " + m.getName());
			}
		
			if (optionsAnnotation != null) {
				adminOnly = optionsAnnotation.adminOnly();
				allowsDisabledAccount = optionsAnnotation.allowDisabledAccount();
			}
			
			String javaName = m.getName();
			if (javaName.startsWith("get")) {
				requiresPost = false;
				name = Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4);
			} else if (javaName.startsWith("do")) {
				requiresPost = true;
				name = Character.toLowerCase(javaName.charAt(2)) + javaName.substring(3);
			} else {
				throw new RuntimeException("http method must start with get or do");
			}
			
			contentTypes = EnumSet.noneOf(HttpResponseData.class);
			for (HttpResponseData r : contentAnnotation.value()) {
				contentTypes.add(r);
			}
			// lock it down
			contentTypes = Collections.unmodifiableSet(contentTypes);
			
			if (contentTypes.isEmpty()) 
				throw new RuntimeException("method has no return types specified " + m.getName());
			
			Class<?> args[] = m.getParameterTypes();
			if (args.length == 0)
				throw new RuntimeException("method doesn't take any args! " + m.getName());

			int i = 0;
			if (OutputStream.class.isAssignableFrom(args[i])) {
				needsOutputStream = true;
				++i;
			} else if (XmlBuilder.class.isAssignableFrom(args[i])) {
				needsXmlBuilder = true;
				++i;
			}
			
			boolean neverReturnsData = contentTypes.size() == 1 &&
				contentTypes.contains(HttpResponseData.NONE); 
			
			if (neverReturnsData && (needsOutputStream || needsXmlBuilder)) {
				throw new RuntimeException("method never returns data but has an output arg " + m.getName());
			}
			if (!neverReturnsData && !(needsOutputStream || needsXmlBuilder)) {
				throw new RuntimeException("method returns data but has no OutputStream or XmlBuilder arg " + m.getName());
			}
			
			if (i < args.length && HttpResponseData.class.isAssignableFrom(args[i])) {
				needsRequestedContentType = true;
				++i;
			}
			
			if (contentTypes.size() > 1 && !needsRequestedContentType) {
				throw new RuntimeException("method must have HttpResponseData arg to specify which content type since it supports multiple " + m.getName()); 
			}
			
			if (i < args.length && Viewpoint.class.isAssignableFrom(args[i])) {
				needsViewpoint = true;
				if (UserViewpoint.class.isAssignableFrom(args[i])) {
					needsUserViewpoint = true;
				}
				++i;
			}
			
			if (args.length != i + paramsAnnotation.value().length) {
				throw new RuntimeException("method " + m.getName() + " should have params " + Arrays.toString(paramsAnnotation.value()));
			}
			
			params = new ArrayList<HttpMethodParam>(paramsAnnotation.value().length);
			
			for (String pname : paramsAnnotation.value()) {
				
				Marshaller marshaller = getRepository().lookupMarshaller(args[i]);
				if (marshaller == null)
					throw new RuntimeException("don't know how to marshal argument to " + m.getName() + " of type " + args[i].getName());
				
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
				
				params.add(new HttpMethodParam(pname, isOptional, marshaller));
				++i;
			}
			// lock it down
			params = Collections.unmodifiableList(params);
		}

		public String getName() {
			return name;
		}

		public boolean isRequiresPost() {
			return requiresPost;
		}

		public boolean isAdminOnly() {
			return adminOnly;
		}

		public boolean isAllowsDisabledAccount() {
			return allowsDisabledAccount;
		}

		public Set<HttpResponseData> getContentTypes() {
			return contentTypes;
		}

		public boolean isNeedsOutputStream() {
			return needsOutputStream;
		}

		public boolean isNeedsRequestedContentType() {
			return needsRequestedContentType;
		}

		public boolean isNeedsUserViewpoint() {
			return needsUserViewpoint;
		}

		public boolean isNeedsViewpoint() {
			return needsViewpoint;
		}

		public boolean isNeedsXmlBuilder() {
			return needsXmlBuilder;
		}

		public List<HttpMethodParam> getParams() {
			return params;
		}
	}
	
	public HttpMethodsServlet2() {
	
	}
	
	private void handleApiDocsIndex(HttpServletRequest request,
				HttpServletResponse response) throws HttpException, IOException {
		// probably we should just use a jsp for this sooner or later
		XmlBuilder xml = new XmlBuilder();
		xml.appendHtmlHead("API Index");
		xml.openElement("body");
		
		xml.openElement("div");
		xml.appendTextNode("h2", "Available Methods");
		
		xml.openElement("ul");
		for (HttpMethod m : getRepository().getMethods()) {
			xml.openElement("li");
			xml.appendTextNode("a", m.getName(), "href",
					"/api-docs/" + m.getName());
			xml.closeElement();
		}
		xml.closeElement();
		xml.closeElement();
		
		xml.closeElement();
		
		response.setContentType("text/html");
		response.getOutputStream().write(xml.getBytes());
	}

	private void handleApiDocsMethod(HttpServletRequest request,
			HttpServletResponse response, String methodName) throws HttpException, IOException {
		HttpMethod method = getRepository().lookupMethod(methodName);
		if (method == null)
			throw new HttpException(HttpResponseCode.NOT_FOUND, "Unknown API method '" + methodName + "'");

		// probably we should just use a jsp for this sooner or later
		XmlBuilder xml = new XmlBuilder();
		xml.appendHtmlHead("API method " + method.getName());
		xml.openElement("body");

		{
			xml.openElement("div");
			
			xml.appendTextNode("h2", method.getName());
			
			{
				xml.openElement("div");			
				String getOrPost;
				if (method.isRequiresPost())
					getOrPost = "Only works with POST, not GET";
				else
					getOrPost = "Works with GET or POST";
				xml.appendTextNode("i", getOrPost);
				xml.closeElement();
			}
 
			{
				xml.openElement("div");			
				String login;
				if (method.isNeedsUserViewpoint())
					login = "Requires a login cookie (cannot be used anonymously)";
				else
					login = "Works anonymously (login cookie need not be provided)";
				xml.appendTextNode("i", login);
				xml.closeElement();
			}
			
			{
				xml.openElement("div");
				xml.appendTextNode("h3", "Parameters");
				
				xml.openElement("ul");
				for (HttpMethodParam p : method.getParams()) {
					xml.openElement("li");
					xml.append(p.getName());
					
					xml.append(" - ");
					
					xml.append(p.getMarshaller().getType().getName());
					
					if (p.isOptional()) {
						xml.appendTextNode("i", "- (optional)");
					}
				}
				xml.closeElement();
				
				xml.closeElement();
			}
			
		
			xml.closeElement();
		}
		
		xml.closeElement();
		
		response.setContentType("text/html");
		response.getOutputStream().write(xml.getBytes());		
	}
	
	private boolean handleApiDocs(HttpServletRequest request, HttpServletResponse response) throws HttpException,
		IOException {
		String requestUri = request.getRequestURI();

		if (requestUri.equals("/api-docs"))
			requestUri = "/api-docs/";
		
		if (!requestUri.startsWith("/api-docs/"))
			return false;
		
		String docsOn = requestUri.substring("/api-docs/".length());
		
		if (docsOn.length() == 0) {
			handleApiDocsIndex(request, response);
		} else {
			handleApiDocsMethod(request, response, docsOn);
		}
		
		return true;
	}
	
	@Override
	public void init() throws ServletException {
		// call this for side effect of loading methods so we get 
		// any errors on startup
		getRepository();
	} 
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException, HumanVisibleException {
		
		setNoCache(response);
		
		// we don't support /api-docs for POST, doesn't make any sense
		
		return null;
	}
	
	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException, HumanVisibleException {
		if (handleApiDocs(request, response))
			return null;
		
		setNoCache(response);
		
		
		
		return null;
	}

	// FIXME we don't especially want a transaction for /api-docs, but if 
	// we start handling actual data we would want one for that
	@Override
	protected boolean requiresTransaction() {
		return false;
	}
}
