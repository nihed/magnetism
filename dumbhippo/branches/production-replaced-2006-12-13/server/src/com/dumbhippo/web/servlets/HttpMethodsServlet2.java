package com.dumbhippo.web.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
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
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.SharedFile;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HttpContentTypes;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpOptions;
import com.dumbhippo.server.HttpParams;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SharedFileSystem;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.XmlMethodErrorCode;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.AnonymousViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.web.DisabledSigninBean;
import com.dumbhippo.web.LoginCookie;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;
import com.dumbhippo.web.WebEJBUtil;
import com.dumbhippo.web.WebStatistics;

/** 
 * This servlet proxies http method invocations ("ajax api") onto 
 * specially-annotated Java methods. It can also spit out docs for 
 * the available API.
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
		private Map<Class<?>,Marshaller<?>> marshallers;
		
		private Map<String,HttpMethod> methods;

		private Map<String,HttpMethod> lowercaseMethods;
		
		private List<HttpMethod> sortedMethods;
		
		HttpMethodRepository() {
			methods = new HashMap<String,HttpMethod>();
			
			marshallers = new HashMap<Class<?>,Marshaller<?>>();
			
			marshallers.put(String.class, new Marshaller<String>() {
	
				public String marshal(Viewpoint viewpoint, String s) throws XmlMethodException {
					return s;
				}
				
				public Class<?> getType() {
					return String.class;
				}
				
			});
			
			marshallers.put(boolean.class, new Marshaller<Boolean>() {
	
				public Boolean marshal(Viewpoint viewpoint, String s) throws XmlMethodException {
					if (s == null)
						return false;
					else if (s.equals("true"))
						return true;
					else if (s.equals("false"))
						return false;
					else
						throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "could not parse boolean value: '" + s + "' use 'true' or 'false'");
				}

				public Class<?> getType() {
					return boolean.class;
				}
				
			});	
			
			marshallers.put(int.class, new Marshaller<Integer>() {
				
				public Integer marshal(Viewpoint viewpoint, String s) throws XmlMethodException {
					if (s == null)
						return -1;
					
					try {
						return Integer.parseInt(s);
					} catch (NumberFormatException e) {
						throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "could not parse integer value: '" + s + "'");
					}
				}

				public Class<?> getType() {
					return int.class;
				}
				
			});	
			
			marshallers.put(User.class, new Marshaller<User>() {
				
				public User marshal(Viewpoint viewpoint, String s) throws XmlMethodException {
					if (s == null)
						return null;
					
					IdentitySpider identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
					try {
						return identitySpider.lookupGuidString(User.class, s);
					} catch (ParseException e) {
						throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "bad userId " + s);
					} catch (NotFoundException e) {
						throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "no such person " + s);
					}
				}

				public Class<?> getType() {
					return User.class;
				}
				
			});	
			
			marshallers.put(Group.class, new Marshaller<Group>() {
				
				public Group marshal(Viewpoint viewpoint, String s) throws XmlMethodException {
					if (s == null)
						return null;
					
					GroupSystem groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
					try {
						return groupSystem.lookupGroupById(viewpoint, s);
					} catch (NotFoundException e) {
						throw new XmlMethodException(XmlMethodErrorCode.UNKNOWN_GROUP, "Unknown group '" + s + "'");
					}
				}

				public Class<?> getType() {
					return Group.class;
				}
				
			});
			
			marshallers.put(Post.class, new Marshaller<Post>() {
				
				public Post marshal(Viewpoint viewpoint, String s) throws XmlMethodException {
					if (s == null)
						return null;
					
					PostingBoard postingBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
					try {
						return postingBoard.loadRawPost(viewpoint, new Guid(s));
					} catch (ParseException e) {
						throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "bad postId " + s);
					} catch (NotFoundException e) {
						throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "no such post " + s);
					}
				}

				public Class<?> getType() {
					return Post.class;
				}
				
			});
			
			marshallers.put(UserBlockData.class, new Marshaller<UserBlockData>() {
				
				public UserBlockData marshal(Viewpoint viewpoint, String s) throws XmlMethodException {
					if (s == null)
						return null;
					
					if (!(viewpoint instanceof UserViewpoint)) {
						throw new XmlMethodException(XmlMethodErrorCode.NOT_LOGGED_IN, "This method requires login");
					}
					
					UserViewpoint userViewpoint = (UserViewpoint) viewpoint;
					
					Stacker stacker = WebEJBUtil.defaultLookup(Stacker.class);
					try {
						return stacker.lookupUserBlockData(userViewpoint, new Guid(s));
					} catch (ParseException e) {
						throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "bad block id " + s);
					} catch (NotFoundException e) {
						throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "no such block " + s);
					}
				}

				public Class<?> getType() {
					return UserBlockData.class;
				}
				
			});
			
			marshallers.put(URL.class, new Marshaller<URL>() {
				public URL marshal(Viewpoint viewpoint, String s) throws XmlMethodException {
					if (s == null)
						return null;
					s = s.trim();
					URL url;
					try {
						url = new URL(s);
					} catch (MalformedURLException e) {
						if (!s.startsWith("http://")) {
							// let users type just "example.com" instead of "http://example.com"
							return marshal(viewpoint, "http://" + s);	
						} else {
							throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "Invalid URL: '" + s + "'");
						}
					}
					return url;
				}

				public Class<?> getType() {
					return URL.class;
				}
			});
			
			marshallers.put(SharedFile.class, new Marshaller<SharedFile>() {
				public SharedFile marshal(Viewpoint viewpoint, String s) throws XmlMethodException {
					if (s == null)
						return null;
					
					SharedFileSystem sharedFileSystem = WebEJBUtil.defaultLookup(SharedFileSystem.class);
					try {
						return sharedFileSystem.lookupFile(viewpoint, new Guid(s), true);
					} catch (ParseException e) {
						throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "bad file id " + s);
					} catch (NotFoundException e) {
						throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "no such file " + s);
					}
				}

				public Class<?> getType() {
					return SharedFile.class;
				}
			});
			
			marshallers.put(Guid.class, new Marshaller<Guid>() {
				public Guid marshal(Viewpoint viewpoint, String s) throws XmlMethodException {
					if (s == null)
						return null;
					
					try {
						return new Guid(s);
					} catch (ParseException e) {
						throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
					}
				}

				public Class<?> getType() {
					return Guid.class;
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
			
			// this is really for legacy javascript, studlyCaps can be used 
			// now
			lowercaseMethods = new HashMap<String,HttpMethod>();
			for (HttpMethod m : methods.values()) {
				lowercaseMethods.put(m.getName().toLowerCase(), m);
			}
		}
	}
	
	private interface Marshaller<BoxedArgType>  {
		/**
		 * Marshal from a string parameter to a Java method argument
		 * @param viewpoint viewpoint of the request
		 * @param s string from http request, or null if param not provided
		 * 
		 * @return the marshaled object (null is allowed)
		 * @throws XmlMethodException thrown if the string can't be parsed
		 */
		public BoxedArgType marshal(Viewpoint viewpoint, String s) throws XmlMethodException;
		
		/**
		 * Gets the unboxed type that we marshal to; not templatized
		 * since unboxed types can't be used in generics
		 * @return the unboxed type
		 */
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
		private boolean invalidatesSession;
		private boolean needsOutputStream;
		private boolean needsXmlBuilder;
		private boolean needsRequestedContentType;
		private boolean needsViewpoint;
		private boolean needsUserViewpoint;
		private Set<HttpResponseData> contentTypes;
		private boolean adminOnly;
		private boolean allowsDisabledAccount;
		private boolean requiresTransaction;
		private Method method;
		
		HttpMethod(Method m,
				   HttpContentTypes contentAnnotation) {
			
			this.method = m;
			
			HttpParams paramsAnnotation = m.getAnnotation(HttpParams.class);
			HttpOptions optionsAnnotation = m.getAnnotation(HttpOptions.class);
			
			if (paramsAnnotation == null) {
				throw new RuntimeException("missing params annotation on " + m.getName());
			}
		
			if (optionsAnnotation != null) {
				adminOnly = optionsAnnotation.adminOnly();
				allowsDisabledAccount = optionsAnnotation.allowDisabledAccount();
				invalidatesSession = optionsAnnotation.invalidatesSession();
				requiresTransaction = optionsAnnotation.transaction();
			} else {
				requiresTransaction = true;
			}
			
			if (m.getReturnType() != void.class)
				throw new RuntimeException("HTTP method " + m.getName() + " must return void not "
						+ m.getReturnType().getCanonicalName());
			
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
			
			if (contentTypes.contains(HttpResponseData.XML) &&
					contentTypes.contains(HttpResponseData.XMLMETHOD)) {
				throw new RuntimeException("Can't return both hand-coded XML and XMLMETHOD style XML from the same API call " + m.getName());
			}
			
			if (contentTypes.isEmpty()) 
				throw new RuntimeException("method has no return types specified " + m.getName());
			
			// lock it down
			contentTypes = Collections.unmodifiableSet(contentTypes);
			
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
			if (!neverReturnsData && allowsDisabledAccount) {
				throw new RuntimeException("account-disabling methods can't return data since the current code will try setting a cookie after the method runs");
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

		public boolean isRequiresTransaction() {
			return requiresTransaction;
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
		
		public Method getMethod() {
			return method;
		}

		public boolean isInvalidatesSession() {
			return invalidatesSession;
		}
	}
	
	public HttpMethodsServlet2() {
	
	}
	
	interface MethodFilter {
		public boolean included(HttpMethod m);
	}
	
	static private void appendIndexSection(XmlBuilder xml,
			String title,
			Collection<HttpMethod> methods,
			MethodFilter filter) {
		xml.appendTextNode("h2", title);
		
		xml.openElement("ul");
		for (HttpMethod m : methods) {
			if (filter.included(m)) {
				xml.openElement("li");
				xml.appendTextNode("a", m.getName(), "href",
						"/api-docs/" + m.getName());
				xml.closeElement();
			}
		}
		xml.closeElement();		
	}
	
	static private void handleApiDocsIndex(HttpServletRequest request,
				HttpServletResponse response) throws HttpException, IOException {
		// probably we should just use a jsp for this sooner or later
		XmlBuilder xml = new XmlBuilder();
		xml.appendHtmlHead("API Index");
		xml.openElement("body");
		
		xml.openElement("div");
		{
			Collection<HttpMethod> methods = getRepository().getMethods();
			appendIndexSection(xml, "Anonymously-callable Methods", methods,
				new MethodFilter() {
					public boolean included(HttpMethod m) {
						return !m.isNeedsUserViewpoint() && !m.isAdminOnly();
					}
			});
			appendIndexSection(xml, "Login-required Methods", methods,
				new MethodFilter() {
					public boolean included(HttpMethod m) {
						return m.isNeedsUserViewpoint() && !m.isAdminOnly();
					}
			});
			appendIndexSection(xml, "Administrator-only Methods", methods,
				new MethodFilter() {
					public boolean included(HttpMethod m) {
						return m.isAdminOnly();
					}
			});
		}
		xml.closeElement();
 	
		xml.closeElement();
		
		response.setContentType("text/html");
		response.getOutputStream().write(xml.getBytes());
	}

	static private void handleApiDocsMethod(HttpServletRequest request,
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
			
			if (method.isAdminOnly()) {
				xml.openElement("div");			
				xml.appendTextNode("i", "Requires administrator privileges");
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
						xml.appendTextNode("i", " - (optional)");
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
	
	static private String arrayToStringXmlBuilderWorkaround(Object[] array) {
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

	/**
	 * This should run the method and write successful output to the output stream.
     *
	 * The caller will write any exception to the output stream, so this method
	 * should just throw the exception (as an XmlMethodException).
	 * 
	 * The caller should take care of side effects and sanity checks, this 
	 * method should only marshal args and run the method. This method does
	 * do some checks that relate to args, such as those that use the viewpoint.
	 * 
	 * Note that checks for the validity of the method itself and its annotations
	 * should be done when the method is initially loaded, not now on each invocation.
	 * 
	 * @param m
	 * @param requestedContentType
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws XmlMethodException
	 */
	static private void invokeMethod(HttpMethod m, HttpResponseData requestedContentType, HttpServletRequest request, HttpServletResponse response) throws IOException, XmlMethodException {
		Class<?> iface = m.getMethod().getDeclaringClass();
		Object instance = WebEJBUtil.defaultLookup(iface);
		Method javaMethod = m.getMethod();
		
		OutputStream out = response.getOutputStream();

		if (requestedContentType != HttpResponseData.NONE)
			response.setContentType(requestedContentType.getMimeType());
		
		SigninBean signin = SigninBean.getForRequest(request);
		UserViewpoint userViewpoint = null;
		
		// if the method doesn't specifically allow disabled accounts, we treat 
		// the request as anonymous instead of as from a user viewpoint
		
		if (signin instanceof UserSigninBean) {
			userViewpoint = ((UserSigninBean)signin).getViewpoint();
		} else if (m.isAllowsDisabledAccount() && 
				signin instanceof DisabledSigninBean) {
			userViewpoint = new UserViewpoint(((DisabledSigninBean)signin).getDisabledUser());
		}
		
		if (userViewpoint == null && m.isNeedsUserViewpoint()) {
			throw new XmlMethodException(XmlMethodErrorCode.NOT_LOGGED_IN, "You need to be signed in to call this");
		}
		
		if (m.isAdminOnly()) {
			IdentitySpider spider = EJBUtil.defaultLookup(IdentitySpider.class);
			if (userViewpoint == null || !spider.isAdministrator(userViewpoint.getViewer()))
				throw new XmlMethodException(XmlMethodErrorCode.FORBIDDEN, "You need administrator privileges to do this");
		}
		
		Viewpoint viewpoint = null;
		if (userViewpoint != null)
			viewpoint = userViewpoint;
		else
			viewpoint = AnonymousViewpoint.getInstance();
  
		// FIXME allow an XmlBuilder arg instead of output stream for 
		// HttpResponseData.XML as well as XMLMETHOD
		XmlBuilder xml = null;
		if (m.isNeedsXmlBuilder() && requestedContentType == HttpResponseData.XMLMETHOD) {
			xml = new XmlBuilder();
			xml.appendStandaloneFragmentHeader();
			xml.openElement("rsp", "stat", "ok");
		}
		
		int argc = m.getParams().size();
		if (m.isNeedsOutputStream())
			++argc;
		if (m.isNeedsXmlBuilder())
			++argc;
		if (m.isNeedsRequestedContentType())
			++argc;
		if (m.isNeedsViewpoint())
			++argc;
		
		Object[] argv = new Object[argc];
		int i = 0;
		if (m.isNeedsOutputStream()) {
			argv[i] = out;
			++i;
		} 
		
		if (m.isNeedsXmlBuilder()) {
			// note xml may just be null here e.g. if a method supports both 
			// XMLMETHOD and NONE
			argv[i] = xml;
			++i;
		}
		
		if (m.isNeedsRequestedContentType()) {
			argv[i] = requestedContentType;
			++i;
		}
		
		if (m.isNeedsViewpoint()) {
			argv[i] = viewpoint;
			++i;
		}
		
		for (HttpMethodParam param : m.getParams()) {
			String value = request.getParameter(param.getName());
			if (value == null && !param.isOptional()) {
				throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT,
						"Parameter " + param.getName() + " is required");
			}
			// if value is null for an optional param, the marshaller is supposed
			// to pass it through appropriately.
			argv[i] = param.getMarshaller().marshal(viewpoint, value);
			++i;
		}
		
		try {
			if (logger.isDebugEnabled()) {						
				String showArgs = arrayToStringXmlBuilderWorkaround(argv);
				// suppress plaintext password from appearing in log
				if (javaMethod.getName().equals("setPassword"))
					showArgs = "[SUPPRESSED FROM LOG]";
				logger.debug("Invoking method {} with args {}", javaMethod.getName(), showArgs);
			}
			javaMethod.invoke(instance, argv);
		} catch (IllegalArgumentException e) {
			logger.error("invoking method on http methods bean, illegal argument", e);
			throw new XmlMethodException(XmlMethodErrorCode.INTERNAL_SERVER_ERROR, "Internal server error");
		} catch (IllegalAccessException e) {
			logger.error("invoking method on http methods bean, illegal access", e);
			throw new XmlMethodException(XmlMethodErrorCode.INTERNAL_SERVER_ERROR, "Internal server error");
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			Throwable rootCause = ExceptionUtils.getRootCause(e);
			
			WebStatistics.getInstance().incrementHttpMethodErrors();
			
			// HumanVisibleException is a legacy thing; it's useless 
			// from an http method since it redirects to the error page.
			// We convert it to XmlMethodException
			if (cause instanceof HumanVisibleException) {
				HumanVisibleException visibleException = (HumanVisibleException) cause;
				throw new XmlMethodException(XmlMethodErrorCode.FAILED, visibleException.getMessage());
			} else if (cause instanceof XmlMethodException){
				XmlMethodException methodException = (XmlMethodException) cause;
				throw methodException;
			} else if (cause instanceof IOException) {
				IOException ioException = (IOException) cause;
				throw ioException;
			} else {
				logger.error("Exception root cause is {} message: {}", rootCause.getClass().getName(), rootCause.getMessage());
				logger.error("invoking method on http methods bean, unexpected", e);
				throw new XmlMethodException(XmlMethodErrorCode.INTERNAL_SERVER_ERROR, "Internal server error");				
			}
		}
		
		if (xml != null) {
			// content type already set at the top of the method
			xml.closeElement();
			byte[] bytes = xml.getBytes();
			response.setContentLength(bytes.length);
			out.write(bytes);
		}

		// The point of the allowDisabledUser annotations is to allow methods that
		// take a user from the disabled state to the enabled state; once we are
		// enabled, we need to persistant cookies, so we check that here. This
		// is a little hacky, but simpler than creating custom servlets.
		//
		// Note that this won't work well with methods that have write
		// output, since output may have already been written, and it
		// will be too late to set cookies. So we disallow that when first 
		// scanning the method.
		if (m.isAllowsDisabledAccount()) {
			SigninBean.updateAuthentication(request, response);
		}
		
		out.flush();
		out.close();
		logger.debug("Reply for {} sent", m.getName());
	}
	
	static private void writeXmlMethodError(HttpServletResponse response, String code, String message) throws IOException {
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement("rsp", "stat", "fail");
		xml.appendTextNode("err", "", "code", code, "msg", message);
		xml.closeElement();
		response.setContentType(HttpResponseData.XMLMETHOD.getMimeType());
		byte[] bytes = xml.getBytes();
		response.setContentLength(bytes.length);
		OutputStream out = response.getOutputStream();
		out.write(bytes);
		out.flush();
		out.close();
	}
	
	static private void writeXmlMethodError(HttpServletResponse response, XmlMethodException exception) throws IOException {
		writeXmlMethodError(response, exception.getCodeString(), exception.getMessage());
	}
	
	private interface RequestHandler {
		public boolean getNoCache();
		public boolean getRequiresTransaction();
		public void handle(HttpServletRequest request, HttpServletResponse response, boolean isPost)
			throws HttpException, IOException;
	}
	
	private static class HttpMethodRequestHandler implements RequestHandler {

		private String typeDir;
		private HttpMethod method;
		
		HttpMethodRequestHandler(String typeDir, HttpMethod method) {
			this.typeDir = typeDir;
			this.method = method;
		}
		
		public boolean getNoCache() {
			return true;
		}
		
		public boolean getRequiresTransaction() {
			return method.isRequiresTransaction();
		}

		public void handle(HttpServletRequest request, HttpServletResponse response, boolean isPost) throws HttpException, IOException {
			HttpResponseData requestedContentType;
			if (typeDir.equals("xml")) {
				// gets overwritten with XMLMETHOD later if appropriate
				requestedContentType = HttpResponseData.XML;
			} else if (typeDir.equals("text"))
				requestedContentType = HttpResponseData.TEXT;
			else if (isPost && typeDir.equals("action"))
				requestedContentType = HttpResponseData.NONE;
			else {
				throw new HttpException(HttpResponseCode.NOT_FOUND,
						"Don't know about URI path /" + typeDir + " , only /xml, /text for GET plus /action for POST only)");
			}

			if (requestedContentType == HttpResponseData.XML && 
					method.getContentTypes().contains(HttpResponseData.XMLMETHOD))
				requestedContentType = HttpResponseData.XMLMETHOD;
			
			if (!method.getContentTypes().contains(requestedContentType)) {
				throw new HttpException(HttpResponseCode.NOT_FOUND, "Wrong content type requested "
						+ requestedContentType + " valid types for method are " + method.getContentTypes());
			}
			
			if (!isPost && method.isRequiresPost())
				throw new HttpException(HttpResponseCode.BAD_REQUEST, "Method only works via POST not GET");
			
			try {
				invokeMethod(method, requestedContentType, request, response);
			} catch (XmlMethodException e) {
				WebStatistics.getInstance().incrementHttpMethodErrors();
				
				if (requestedContentType == HttpResponseData.XMLMETHOD) {
					writeXmlMethodError(response, e);
					return;
				} else {
					throw new HttpException(HttpResponseCode.BAD_REQUEST,
							e.getCodeString() + ": " + e.getMessage());
				}
			}
			
			////// Note that we always throw or return on exception... so this code 
			////// runs only on method success
			WebStatistics.getInstance().incrementHttpMethodsServed();
			
			if (method.isInvalidatesSession()) {
				HttpSession sess = request.getSession(false);
				if (sess != null)
					sess.invalidate();	
			}			
		}
		
	}
	
	private String[] parseUri(String uri) {
		
		if (uri.length() < 4) { // "/a/b".length() == 4
			logger.debug("URI is too short to be valid, should be of the form /typeprefix/methodname, e.g. /xml/frobate");
			return null;
		}
		
		// ignore trailing /
		if (uri.endsWith("/"))
			uri = uri.substring(0, uri.length()-1);
		
		// split into the two components, the result is { "", "xml", "frobate" }
		String[] ret = uri.split("/");
		if (ret.length != 3) {
			logger.debug("All URIs are of the form /typeprefix/methodname, e.g. /xml/frobate, split into: " + Arrays.toString(ret));
			return null;
		}
		
		return new String[] { ret[1], ret[2] };
	}
	
	private RequestHandler tryHttpRequest(HttpServletRequest request) {
		String requestUri = request.getRequestURI(); 
		String[] uriComponents = parseUri(requestUri);
		if (uriComponents == null)
			return null;
		String requestedMethod = uriComponents[1];

		HttpMethodRepository repo = getRepository();
		HttpMethod m = repo.lookupMethod(requestedMethod);
		if (m == null)
			return null;
		else
			return new HttpMethodRequestHandler(uriComponents[0], m);
	}
	
	private RequestHandler tryLoginRequests(HttpServletRequest request) {
		if (!(request.getRequestURI().equals("/text/dologin") &&
				request.getMethod().toUpperCase().equals("POST"))
				&& !request.getRequestURI().equals("/text/checklogin")) {
			return null;
		} else {
			return new RequestHandler() {
				public boolean getNoCache() {
					return true;
				}				
				
				public boolean getRequiresTransaction() {
					return true;
				}

				public void handle(HttpServletRequest request, HttpServletResponse response, boolean isPost)
					throws HttpException, IOException {
					// special method that magically causes us to look at your cookie and log you 
					// in if it's set, then return person you're logged in as or "false"		
						
					User user = getUser(request);
						
					response.setContentType("text/plain");
					OutputStream out = response.getOutputStream();
					if (user != null)
						out.write(user.getId().getBytes());
					else
						out.write("false".getBytes());
					out.flush();					
				}
				
			};
		}
	}

	private RequestHandler trySignoutRequest(HttpServletRequest request) {
		if (!request.getRequestURI().equals("/action/signout") ||
			!request.getMethod().toUpperCase().equals("POST")) {
			return null;
		} else {
			return new RequestHandler() {
				public boolean getNoCache() {
					return true;
				}
				
				public boolean getRequiresTransaction() {
					return false;
				}
				
				public void handle(HttpServletRequest request, HttpServletResponse response, boolean isPost)
					throws HttpException, IOException {
					HttpSession session = request.getSession();
					if (session != null)
						session.invalidate();		
					
					// FIXME we need to drop the Client object when we do this,
					// both to save our own disk space, and in case someone stole the 
					// cookie.
					
					response.addCookie(LoginCookie.newDeleteCookie());
				}
			};
		}
	}

	private RequestHandler tryApiDocs(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
	
		if (requestUri.equals("/api-docs"))
			requestUri = "/api-docs/";
	
		if (!requestUri.startsWith("/api-docs/"))
			return null;
	
		final String docsOn = requestUri.substring("/api-docs/".length());

		return new RequestHandler() {

			public boolean getNoCache() {
				return false;
			}
			
			public boolean getRequiresTransaction() {
				return false;
			}

			public void handle(HttpServletRequest request, HttpServletResponse response, boolean isPost)
				throws HttpException, IOException {
				if (docsOn.length() == 0) {
					handleApiDocsIndex(request, response);
				} else {
					handleApiDocsMethod(request, response, docsOn);
				}
			}
			
		};
	}
	
	@Override
	public void init() throws ServletException {
		// call this for side effect of loading methods so we get 
		// any errors on startup
		getRepository();
	} 
	
	private void doRequest(HttpServletRequest request, HttpServletResponse response, boolean isPost)
		throws HttpException, IOException {
		RequestHandler handler = (RequestHandler) request.getAttribute("request-handler");
		if (handler == null) {
			logger.debug("Found no handler for url '{}'", request.getRequestURI());
			throw new HttpException(HttpResponseCode.NOT_FOUND, "unknown URI");
		}
		if (handler.getNoCache())
			setNoCache(response);
		handler.handle(request, response, isPost);
	}
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException {
		
		doRequest(request, response, true);

		return null;
	}
	
	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException {

		doRequest(request, response, false);
		
		return null;
	}

	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		
		// the idea is to do the "request analysis" only once, rather than both 
		// here and also in the doGet/doPost
		
		RequestHandler handler = null;
		boolean isPost = request.getMethod().toUpperCase().equals("POST");
		if (isPost) {
			if (handler == null)
				handler = tryLoginRequests(request);
			if (handler == null)
				handler = trySignoutRequest(request);
		} else {
			if (handler == null)
				handler = tryApiDocs(request);
		}
		if (handler == null)
			handler = tryHttpRequest(request);
		
		if (handler != null) {
			request.setAttribute("request-handler", handler);
			return handler.getRequiresTransaction();
		} else {
			// we're going to throw an error later
			return false;
		}
	}
}
