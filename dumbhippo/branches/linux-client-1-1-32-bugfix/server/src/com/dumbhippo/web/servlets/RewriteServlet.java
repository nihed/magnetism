package com.dumbhippo.web.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
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
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.ServerStatus;
import com.dumbhippo.server.impl.ConfigurationBean;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.web.JavascriptResolver;
import com.dumbhippo.web.RewrittenRequest;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;
import com.dumbhippo.web.WebEJBUtil;
import com.dumbhippo.web.WebStatistics;

public class RewriteServlet extends HttpServlet {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(RewriteServlet.class);	
	
	static final long serialVersionUID = 1;
	
	static private boolean xmppEnabled = true;
	
	static public void setXmppEnabled(boolean enabled) {
		xmppEnabled = enabled;
	}
	
	static public boolean getXmppEnabled() {
		return xmppEnabled;
	}
	
	private boolean stealthMode;
	private Set<String> requiresSignin;
	private Set<String> requiresSigninStealth;
	private Set<String> noSignin;
	private Map<String, Integer> jspPages;
	private Set<String> htmlPages;
	private String buildStamp;
	
	private List<String> psaLinks; // used to choose a random one
	private int nextPsa;
	
	private JavascriptResolver jsResolver;
	
	private ServletContext context;
	private ServerStatus serverStatus;
	
	private File buildstampFile;
	private ExecutorService buildstampThread;
	private long buildstampFileLastModified;
	private Lock buildstampLock;
	private Condition buildstampCondition;
	private int webVersion;
	
	private FaviconHandler faviconHandler;
	
	private Guid getSigninGuid(HttpServletRequest request) {
		SigninBean signin = SigninBean.getForRequest(request);
		if (signin.isValid()) {
			Viewpoint viewpoint = signin.getViewpoint();
			if (viewpoint instanceof UserViewpoint)
				return ((UserViewpoint) viewpoint).getViewer().getGuid();
		}
		return null;
	}
	
    private String checkBuildStamp(String relativePath) {
		int firstSlash = relativePath.indexOf('/', 1);
		if (firstSlash < 0)
			return null;

		int buildStampLength = buildStamp.length();
		if (relativePath.regionMatches(firstSlash + 1, buildStamp, 0, buildStampLength)) {
			return (relativePath.substring(0, firstSlash) + relativePath.substring(firstSlash + 1 + buildStampLength));
		} else {
			return null;
		}
	}
    
    private static final String DEFAULT_IMAGE_SIZE = "60";
    
    private boolean needsImageSize(String relativePath) {
		return relativePath.startsWith("/images2/user_pix1/") ||
			   relativePath.startsWith("/images2/group_pix1/");
    }
    
    private String checkImageSize(HttpServletRequest request, String relativePath) {
    	if (!needsImageSize(relativePath))
    		return null;

		String size = DEFAULT_IMAGE_SIZE;
    	String sizeParameter = request.getParameter("size");
    	if (sizeParameter != null && AbstractSmallImageServlet.isValidSize(sizeParameter))
        	size = sizeParameter;
    	
    	int lastSlash = relativePath.lastIndexOf("/");
    	String newPath = relativePath.substring(0, lastSlash + 1) + size + relativePath.substring(lastSlash);
    	
    	return newPath;
    }
     
    private synchronized String getNextPsa() {
    	if (nextPsa >= psaLinks.size())
    		nextPsa = 0;
    	String s = psaLinks.get(nextPsa);
    	++nextPsa;
    	return s;
    }

    private boolean canBusyRedirect(HttpServletRequest request) {
	// The busy page is displayed with /error, so redirecting error
	// causes an infinite loop. Plus, redirecting /error will make it
	// hard to tell what is going on.
	
	String path = request.getServletPath();
	return !path.equals("/error");
    }
    
    private String getVersionedJspPath(String name) {
		Integer version = jspPages.get(name);
		String suffix = version.intValue() > 1 ? version.toString() : ""; 	
		return "/jsp" + suffix + "/" + name + ".jsp";
    }
	
	private void handleVersionedJsp(HttpServletRequest request, HttpServletResponse response, String name) throws IOException, ServletException {
		// We can't use RewrittenRequest for JSP's because it breaks the
		// handling of <jsp:forward/> and is generally unreliable
		handleJsp(request, response, getVersionedJspPath(name));		
	}
    
	public void handleJsp(HttpServletRequest request, HttpServletResponse response, String newPath) throws IOException, ServletException {
		// Instead of just forwarding JSP's to the right handler, we surround
		// them in a transaction; this doesn't have anything to do with 
		// making atomic modifications - JSP pages are pretty much entirely
		// readonly. Instead, the point is to get the entire page to use
		// a single Hibernate session. This improves performance, since
		// persistance beans are cached across the session, and also means
		// that persistance beans returned to the web tier won't be detached.
		//
		// While we are add it, we time the page for performancing monitoring
		
		// If the server says it's too busy, just redirect to a busy page
	    if (serverStatus.isTooBusy() && canBusyRedirect(request)) {
	    	WebStatistics.getInstance().incrementWebTooBusyCount();
			context.getRequestDispatcher(getVersionedJspPath("busy")).forward(request, response);
			return;
		}
		
		boolean transactionCreated = false;
		long startTime = System.currentTimeMillis();
		UserTransaction tx;
		try {
			InitialContext initialContext = new InitialContext();
			tx = (UserTransaction)initialContext.lookup("UserTransaction");
		} catch (NamingException e) {
			throw new RuntimeException("Cannot create UserTransaction");
		}
		
		try {
			if (tx.getStatus() == Status.STATUS_NO_TRANSACTION) {
				try {
					tx.begin();
					transactionCreated = true;
				} catch (NotSupportedException e) {
					throw new RuntimeException("Error starting transaction", e); 
				} catch (SystemException e) {
					throw new RuntimeException("Error starting transaction", e); 
				}
			}
		} catch (SystemException e) {
			throw new RuntimeException("Error getting transaction status", e);
		}
		
		try {
			request.setAttribute("webVersion", webVersion);

			SigninBean signin = SigninBean.getForRequest(request);

			if (signin.isValid()) {
				// When the user is logged in, we want to disable all caching of
				// our responses; since we don't specify an Expires header, browsers
				// would normally revalidate with an If-Modified request anyways, but 
				// IE in some cases will used cached content without checking 
				// (for example, when using the forward/back buttons). That breaks how 
				// we do action links that cause the page to reload. Preventing that 
				// caching will make back/forward a little slower, but as long as we 
				// keep our page load times snappy it isn't a big deal.				
				response.setHeader("Cache-Control", "no-cache");
				
				// Also update their last web login time
				UserSigninBean userSignin = (UserSigninBean) signin;
				User user = userSignin.getUser();
				AccountSystem accountSystem = EJBUtil.defaultLookup(AccountSystem.class);
				accountSystem.updateWebActivity(user);
			}
			
			// Deleting the user from SigninBean means that next time it
			// is accessed, we'll get a copy attached to this hibernate Session
			signin.resetSessionObjects();
			
			context.getRequestDispatcher(newPath).forward(request, response);
			WebStatistics.getInstance().incrementJspPagesServed();
			
			long serveTime = System.currentTimeMillis() - startTime;
			logger.debug("Handled {} in {} milliseconds", newPath, serveTime);
			WebStatistics.getInstance().addPageServeTime(serveTime);
		} catch (Throwable t) {
			WebStatistics.getInstance().incrementJspPageErrors();
			
			try {
				// We don't try to duplicate the complicated EJB logic for whether
				// the transaction should be rolled back; we just roll it back
				// always if an exception occurs. After all, the transaction
				// probably didn't do any writing to start with.
				if (transactionCreated)
					tx.setRollbackOnly();
			} catch (SystemException e) {
				throw new RuntimeException("Error setting transaction for rollback");
			}
			
			if (t instanceof Error)
				throw (Error)t;
			else if (t instanceof RuntimeException)
				throw (RuntimeException)t;
			else
				throw new RuntimeException(t);
		} finally {
			if (transactionCreated) {
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
	}

	@Override
	public void service(HttpServletRequest request,	HttpServletResponse response) throws IOException, ServletException {

		// be sure we only handle appropriate http methods, not e.g. DAV methods.
		// also, appropriately implement OPTIONS method.
		String httpMethod = request.getMethod().toUpperCase();
		if (!(httpMethod.equals("GET") ||
				httpMethod.equals("HEAD") ||
				httpMethod.equals("POST"))) {
			// If an unexpected method is received, the reply is the same as 
			// for the OPTIONS method, but with this error. See the 
			// http spec where it defines 405 Method Not Allowed.
			if (!httpMethod.equals("OPTIONS")) {
				logger.warn("Got unusual method {} on rewrite servlet", httpMethod);
				response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);	
			}
			response.setHeader("Allow", "GET, HEAD, POST, OPTIONS");
			return;
		}
		
		Boolean modifiedPath = false;
		
		String path = request.getServletPath();
		
		// this line of debug is cut-and-pasted over to AbstractServlet also
		logger.debug("--------------- HTTP {} for '{}' content-type=" + request.getContentType(), httpMethod, path);
				
		// see for example http://www.p3pwriter.com/LRN_111.asp
		// This is a partial machine-readable encoding of 
		// the privacy policy that allows our login cookie
		// to work in an iframe when using IE.
		response.setHeader("P3P", "CP=\"CAO PSA OUR\"");
		
		// The root URL is special-cased, we forward it depending
		// on whether the user is signed in and depending on our
		// configuration.
		
		if (path.equals("/")) {
			Guid signinUserGuid = getSigninGuid(request);
			if (signinUserGuid != null)
				response.sendRedirect("/person?who=" + signinUserGuid.toString());
			else if (stealthMode)
				response.sendRedirect("/comingsoon");
			else
				response.sendRedirect("/main");
			return;
		}
		
		// You'd think that we could handle static content with, in servlet-info.xml 
		// <servlet-mapping>
		//    <server-name>default</server-name>
		//    <url-pattern>/javascript/*</url-pattern>
		// </servlet-mapping>
		//
		// But that won't work since the Tomcat default servlet (and our modified version)
		// looks up the resource based on servlet-path, which in the above will be missing 
		// /javascript so we have to handle javascript/ and css/ URLs here. We could
		// hack StaticServlet further from the original code to avoid this.
		
		if (path.equals("/favicon.ico")) {
			// there are some cases where we aren't serving html so there's no 
			// <link type="icon"/> - we normally point browsers to /images/favicon.ico 
			// in the html itself, but we need this for when we don't
			RewrittenRequest rewrittenRequest = new RewrittenRequest(request, "/images2/favicon.ico");
			context.getNamedDispatcher("default").forward(rewrittenRequest, response);
			return;
		} else if (path.startsWith("/javascript/") ||
				path.matches("/css\\d*/.*") ||
				path.matches("/images\\d*/.*") ||
				path.startsWith("/flash/") || 
				path.startsWith("/favicons/")) {
			
			String pathWithoutStamp = checkBuildStamp(path);
			if (pathWithoutStamp != null) {
				AbstractServlet.setInfiniteExpires(response);
				path = pathWithoutStamp;
				modifiedPath = true;
			}
			
			String pathWithImageSize = checkImageSize(request, path);
			if (pathWithImageSize != null) {
				path = pathWithImageSize;
				modifiedPath = true;
			}
			
			if (path.equals("/javascript/config.js")) {
				// config.js is special and handled by a JSP, but it doesn't need
				// our usual error/transaction stuff in handleJsp since it's just text 
				// substitution
				context.getRequestDispatcher(getVersionedJspPath("configjs")).forward(request, response);
			} else if (path.equals("/javascript/whereimat.js")) {
				handleVersionedJsp(request, response, "whereimatjs");
			} else if (path.startsWith("/favicons/")) {
				faviconHandler.service(context, request, response, path, modifiedPath);
			} else if (modifiedPath) {
				RewrittenRequest rewrittenRequest = new RewrittenRequest(request, path);
				context.getNamedDispatcher("default").forward(rewrittenRequest, response);
			} else {
				context.getNamedDispatcher("default").forward(request, response);
			}
			
			return;
		}
		
		String afterSlash = path.substring(1);
		
		SigninBean signin = SigninBean.getForRequest(request);
		boolean doSigninRedirect;
		
		boolean isRequiresSignin = requiresSignin.contains(afterSlash); 
		boolean isRequiresSigninStealth = requiresSigninStealth.contains(afterSlash);
		boolean isNoSignin = noSignin.contains(afterSlash);

		// We force the page to be in one of our configuration parameters,
		// since otherwise, its too easy to forget to update the configuration
		// parameters, even with the warnings we output below.
		if (!isRequiresSignin && !isRequiresSigninStealth && !isNoSignin) {
			logger.warn("Page signin requirements not specified");
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		doSigninRedirect = (isRequiresSignin || (stealthMode && isRequiresSigninStealth)) && 
						   !signin.isValid();

		// If this is a request to one of the pages configured as requiresLogin,
		// and the user isn't signed in, go to /who-are-you, storing the real
		// destination in the query string. This only works for GET, since we'd
		// have to save the POST parameters somewhere.
		
		if (doSigninRedirect && request.getMethod().toUpperCase().equals("GET")) {
			
			String url = response.encodeRedirectURL("/who-are-you?next=" + afterSlash);
			if (stealthMode && requiresSigninStealth.contains(afterSlash)) {
				url = url + "&wouldBePublic=true";
			}

			response.sendRedirect(url);
			return;
		}
		
		// Now handle the primary set of user visible pages, which is a merge
		// of static HTML and JSP's.
		
		// Set variables with rotating PSAs; it's important to avoid 
		// using up "getNextPsa()" with pages that won't use these though,
		// so we get the full rotation. e.g. with 3 PSAs, if you getNextPsa()
		// when loading 2 PSAs on the page itself, then each page load goes through 6 PSAs which
		// means you always skip certain ones on the JSP
		if (!afterSlash.startsWith("psa-")) {
			request.setAttribute("psa1", getNextPsa());
			request.setAttribute("psa2", getNextPsa());
		}
		
		if (!xmppEnabled) {
			if (path.equals("/sharelink")) {
				path = "/sharelink-disabled";
				afterSlash = "sharelink-disabled";
			} else if (path.equals("/chatwindow")) {
				request.setAttribute("disabled", "true");
			}
		}
		
		if (jspPages.containsKey(afterSlash)) {
			handleVersionedJsp(request, response, afterSlash);
		} else if (htmlPages.contains(afterSlash)) {
			String newPath;
			// We could eliminate the use of RewrittenRequest entirely by
			// adding a mapping for *.html to servlet-info.xml
			if (afterSlash.equals("robots.txt"))
				newPath = "/html/robots.txt";
			else
				newPath = "/html" + path + ".html";
			RewrittenRequest rewrittenRequest = new RewrittenRequest(request, newPath);
			context.getNamedDispatcher("default").forward(rewrittenRequest, response);
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
	static private Set<String> getStringSet(ServletConfig config, String param) {
		Set<String> set = new HashSet<String>();
		String s = config.getInitParameter(param);
		if (s != null)
			for (String m : s.split(","))
				set.add(m.trim());
		return set;
	}
	
	private void initBuildStampScan() { 
		URL url = RewriteServlet.class.getResource("buildstamp.properties");
		File file;
		try {
			 file = new File(new URI(url.toExternalForm()));
		} catch (URISyntaxException e) {
			logger.error("Failed to get buildstamp.properties URI", e);
			throw new RuntimeException(e);
		}
		// the lock covers buildstampFile, buildstampFileLastModified, and updating the build stamp
		buildstampLock = new ReentrantLock();
		buildstampCondition = buildstampLock.newCondition();
		buildstampFile = file;
		buildstampFileLastModified = 0;
		
		buildstampThread = ThreadUtils.newSingleThreadExecutor("buildstamp scanner");
		buildstampThread.execute(new Runnable() {
			public void run() {
				logger.debug("Entering build stamp scanner thread");
				
				buildstampLock.lock();
				try {
					while (buildstampFile != null) {
						try {
							loadBuildStamp();
						} catch (IOException e) {
							logger.warn("Build stamp load failed", e);
						}
						try {
							buildstampCondition.await(2, TimeUnit.SECONDS);
						} catch (InterruptedException e) {
						}
					}
				} finally {
					buildstampLock.unlock();
				}
				
				logger.debug("Leaving build stamp scanner thread");
			}
		});
	}
	
	private void stopBuildStampScan() {
		logger.debug("Asking build stamp scanner thread to exit");
		
		buildstampLock.lock();
		try {
			buildstampFile = null;
			// this just saves us the timeout of the scanner thread
			buildstampCondition.signalAll();
		} finally {
			buildstampLock.unlock();
		}
		
		ThreadUtils.shutdownAndAwaitTermination(buildstampThread);

		logger.debug("Done waiting for buildstamp scanner thread, terminated={}", buildstampThread.isTerminated());
		buildstampThread = null;
	}
	
	// when called from the buildstamp scanner thread, we'll already 
	// have the buildstampLock; but we take the lock ourselves also
	// because on startup the scanner thread and the init() method 
	// both call this
	private void loadBuildStamp() throws IOException {
		buildstampLock.lock();
		try {
			if (buildstampFile == null) // indicates that load thread has been asked to shut down
				return;
			
			Properties props = new Properties();
			long modified = buildstampFile.lastModified();
			if (modified == buildstampFileLastModified) {
				return;
			}
			
			logger.debug("New timestamp on buildstamp.properties: {}", modified);
			buildstampFileLastModified = modified;
			
			InputStream str = new FileInputStream(buildstampFile);
			props.load(str);
	
	        buildStamp = props.getProperty("dumbhippo.server.buildstamp");
	        
	        if (buildStamp == null || buildStamp.trim().length() == 0) {
	        	logger.error("buildstamp.properties does not contain build stamp");
	        	// IOException so the calling thread will catch it and not exit;
	        	// this can possibly happen in normal circumstances if the props
	        	// file isn't written atomically
	        	throw new IOException("buildstamp.properties does not contain build stamp");
	        }
	     
			logger.debug("Loaded build stamp '{}'", buildStamp);
	        
	        // We store the builtstamp in the servlet context so we can reference it from JSP pages.
	        // This could change the buildstamp while a jsp and related items are in the process of being served,
	        // but not sure how to avoid that. Should not matter on the production server since 
	        // we won't change the build stamp there.
	        getServletContext().setAttribute("buildStamp", buildStamp);
	        
	        // Now also load the Javascript dependency information, since .js files
	        // might have changed. This makes the whole loadBuildStamp() name on this
	        // method a bit of a misnomer, I guess.
	       
			String jsFileDepsPath = context.getRealPath("/javascript/file-dependencies.txt");
			if (jsFileDepsPath == null)
				throw new IOException("/javascript/file-dependencies.txt not found");
			String jsModuleMapPath = context.getRealPath("/javascript/module-file-map.txt");
			if (jsModuleMapPath == null)
				throw new IOException("/javascript/module-file-map.txt not found");
			
			jsResolver = JavascriptResolver.newInstance("/javascript", buildStamp,
						new File(jsFileDepsPath), new File(jsModuleMapPath));
	        
			getServletContext().setAttribute("jsResolver", jsResolver);
			
			logger.debug("Loaded new Javascript dependency information from {} and {}",
					jsFileDepsPath, jsModuleMapPath);
			
		} finally {
			buildstampLock.unlock();
		}
	}
	
	@Override
	public void init() throws ServletException {
		ServletConfig config = getServletConfig();
		context = config.getServletContext();

		ConfigurationBean.setWebRealPath(new File(context.getRealPath("/")));
		
		serverStatus = WebEJBUtil.defaultLookup(ServerStatus.class);
		
        Configuration configuration = WebEJBUtil.defaultLookup(Configuration.class);
        
		String stealthModeString = configuration.getProperty(HippoProperty.STEALTH_MODE);
		webVersion = Integer.parseInt(configuration.getPropertyFatalIfUnset(HippoProperty.WEB_VERSION));
		
		stealthMode = Boolean.parseBoolean(stealthModeString);
		
		logger.debug("Stealth mode: " + stealthMode);
		
		requiresSignin = getStringSet(config, "requiresSignin");
		requiresSigninStealth = getStringSet(config, "requiresSigninStealth");
		noSignin = getStringSet(config, "noSignin");		
		
		jspPages = new HashMap<String, Integer>();
		for (int i = 1; i <= webVersion; i++) {
			String prefix = "/jsp" + (i == 1 ? "" : Integer.toString(i)) + "/";		
			for (Object o : context.getResourcePaths(prefix)) {
				String path = (String)o;
				if (path.endsWith(".jsp") && path.indexOf('/') != -1)
					jspPages.put(path.substring(prefix.length(), path.length() - 4), i);
			}			
		}
		logger.debug("jsp pages are {}", jspPages);
		
		htmlPages = new HashSet<String>();
		Set htmlPaths = context.getResourcePaths("/html/");
		if (htmlPaths != null) {
			for (Object o : htmlPaths) {
				String path = (String)o;
				if (path.endsWith(".html") && path.indexOf('/') != -1)
					htmlPages.add(path.substring(6, path.length() - 5));
				else if (path.equals("/html/robots.txt")) // special case this special file
					htmlPages.add("robots.txt");
			}
		}		
	
		Set<String> withSigninRequirements = new HashSet<String>(requiresSignin);
		withSigninRequirements.addAll(requiresSigninStealth);
		withSigninRequirements.addAll(noSignin);
		
		for (String p : jspPages.keySet()) {
			if (!withSigninRequirements.contains(p)) {
				if (p.startsWith("psa-"))
					noSignin.add(p);
				else {
					// This warning is generated superfluously on some unused /jsp pages
					// for now
					logger.error(".jsp {} at version {} does not have its signin requirements specified", p, jspPages.get(p));
				}
			}
		}

		for (String p : htmlPages) {
			if (!withSigninRequirements.contains(p)) {
				if (p.startsWith("psa-"))
					noSignin.add(p);
				else {
					logger.error(".html {} does not have its signin requirements specified", p);
				}
			}
		}
		
		for (String p : withSigninRequirements) {
			if (!jspPages.containsKey(p) && !htmlPages.contains(p)) {
				logger.warn("Page '{}' in servlet config is not a .jsp or .html we know about", p);
			}
		}
		
		psaLinks = new ArrayList<String>();
		for (String p : noSignin) {
			if (p.startsWith("psa-"))
				psaLinks.add(p);
		}
		logger.debug("Added {} PSAs: {}", psaLinks.size(), psaLinks);		
        
		faviconHandler = new FaviconHandler();
		
		initBuildStampScan();
		try {
			loadBuildStamp();
		} catch (IOException e) {
			logger.error("Initial build stamp load failed: {}", e.getMessage());
			throw new ServletException("Failed to load build stamp", e);
		}
		
        // Store the server's base URL for reference from JSP pages
        String baseUrl = configuration.getPropertyFatalIfUnset(HippoProperty.BASEURL);
        getServletContext().setAttribute("baseUrl", baseUrl);
        getServletContext().setAttribute("googleAnalyticsKey", configuration.getPropertyFatalIfUnset(HippoProperty.GOOGLE_ANALYTICS_KEY));        
	}
	
	@Override
	public void destroy() {
		stopBuildStampScan();
	}
}
