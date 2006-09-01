package com.dumbhippo.web.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.ServerStatus;
import com.dumbhippo.web.DisabledSigninBean;
import com.dumbhippo.web.RewrittenRequest;
import com.dumbhippo.web.SigninBean;
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
	private Set<String> jspPages;
	private Set<String> jsp2Pages;	
	private Set<String> htmlPages;
	private String buildStamp;
	
	private List<String> psaLinks; // used to choose a random one
	private int nextPsa;
	
	private ServletContext context;
	private ServerStatus serverStatus;
	
	private File buildstampFile;
	private ExecutorService buildstampThread;
	private long buildstampFileLastModified;
	private Lock buildstampLock;
	private Condition buildstampCondition;
	
	private boolean hasSignin(HttpServletRequest request) {
		return SigninBean.getForRequest(request).isValid();
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
			context.getRequestDispatcher("/jsp2/busy.jsp").forward(request, response);
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
			// Deleting the user from SigninBean means that next time it
			// is accessed, we'll get a copy attached to this hibernate Session
			SigninBean.getForRequest(request).resetSessionObjects();
			
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
		
		String newPath = null;
		
		String path = request.getServletPath();
		
		// this line of debug is cut-and-pasted over to AbstractServlet also
		logger.debug("--------------- HTTP {} for '{}' content-type=" + request.getContentType(), request.getMethod(), path);
		
		// Support for legacy /home, main, and /comingsoon URLs;
		// forward them all to the root URL; see next stanza for
		// the special case treatment they will then get
		if (path.equals("/home") || path.equals("/main") || path.equals("/comingsoon")) {
			response.sendRedirect("");
			return;
		}
		
		// The root URL is special-cased, we forward it depending
		// on whether the user is signed in and depending on our
		// configuration.
		
		if (path.equals("/")) {
			if (hasSignin(request))
				handleJsp(request, response, "/jsp2/home.jsp");
			else if (stealthMode)
				handleJsp(request, response, "/jsp2/comingsoon.jsp");
			else
				handleJsp(request, response, "/jsp2/main.jsp");
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
			path.startsWith("/css/") ||
			path.startsWith("/images/") ||
			path.startsWith("/css2/") ||
			path.startsWith("/images2/") ||
			path.startsWith("/flash/")) {
			
			newPath = checkBuildStamp(path);
			if (newPath != null) {
				AbstractServlet.setInfiniteExpires(response);
				path = newPath;
			}
			
			if (path.equals("/javascript/config.js")) {
				// config.js is special and handled by a JSP, but it doesn't need
				// our usual error/transaction stuff in handleJsp since it's just text 
				// substitution
				context.getRequestDispatcher("/jsp/configjs.jsp").forward(request, response);
			} else if (path.equals("/javascript/whereimat.js")) {
				handleJsp(request, response, "/jsp2/whereimatjs.jsp");
			} else if (newPath != null) {
				RewrittenRequest rewrittenRequest = new RewrittenRequest(request, newPath);
				context.getNamedDispatcher("default").forward(rewrittenRequest, response);
			} else {
				context.getNamedDispatcher("default").forward(request, response);
			}
			
			return;
		}
		
		String afterSlash = path.substring(1);
		
		SigninBean signin = SigninBean.getForRequest(request);
		boolean doSigninRedirect;
		
		// we-miss-you and download are special because they convert from
		// DisabledSigninBean to UserSigninBean
		if (afterSlash.equals("we-miss-you")) {
			if ((signin instanceof DisabledSigninBean) && signin.isDisabled())
				doSigninRedirect = false;
			else
				doSigninRedirect = !signin.isValid();
		} else if (afterSlash.equals("download")) {
			if ((signin instanceof DisabledSigninBean) && signin.getNeedsTermsOfUse())
				doSigninRedirect = false;
			else
				doSigninRedirect = !signin.isValid();
		} else if (afterSlash.equals("who-are-you") && (signin instanceof DisabledSigninBean) && signin.getNeedsTermsOfUse()) {
			// don't allow the person to request a sign in link, redirect them to the
			// download page right away
            doSigninRedirect = true;
		} else {
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
		}
		
		// If this is a request to one of the pages configured as requiresLogin,
		// and the user isn't signed in, go to /who-are-you, storing the real
		// destination in the query string. This only works for GET, since we'd
		// have to save the POST parameters somewhere.
		
		if (doSigninRedirect && request.getMethod().toUpperCase().equals("GET")) {
			
			String url;
			if (signin instanceof DisabledSigninBean) {
				DisabledSigninBean disabledSignin = (DisabledSigninBean)signin;
				if (disabledSignin.getNeedsTermsOfUse())
					url = response.encodeRedirectURL("/download?acceptMessage=true");
				else if (disabledSignin.isDisabled())
					url = response.encodeRedirectURL("/we-miss-you?next=" + afterSlash);
				else
					throw new RuntimeException("DisabledSigninBean has no reason for being disabled");
			} else {
				url = response.encodeRedirectURL("/who-are-you?next=" + afterSlash);
				if (stealthMode && requiresSigninStealth.contains(afterSlash)) {
					url = url + "&wouldBePublic=true";
				}
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
				path = "/chatwindow-disabled";
				afterSlash = "chatwindow-disabled";
			}
		}
		
		if (jsp2Pages.contains(afterSlash)) {
			// We can't use RewrittenRequest for JSP's because it breaks the
			// handling of <jsp:forward/> and is generally unreliable.
			newPath = "/jsp2" + path + ".jsp";
			
			handleJsp(request, response, newPath);
			
		} else if (htmlPages.contains(afterSlash)) {
			// We could eliminate the use of RewrittenRequest entirely by
			// adding a mapping for *.html to servlet-info.xml
			if (afterSlash.equals("robots.txt"))
				newPath = "/html/robots.txt";
			else
				newPath = "/html" + path + ".html";
			RewrittenRequest rewrittenRequest = new RewrittenRequest(request, newPath);
			context.getNamedDispatcher("default").forward(rewrittenRequest, response);
		} else if (jspPages.contains(afterSlash)) {
			newPath = "/jsp" + path + ".jsp";
			
			handleJsp(request, response, newPath);
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
		} finally {
			buildstampLock.unlock();
		}
	}
	
	@Override
	public void init() throws ServletException {
		ServletConfig config = getServletConfig();
		context = config.getServletContext();

		serverStatus = WebEJBUtil.defaultLookup(ServerStatus.class);
		
        Configuration configuration = WebEJBUtil.defaultLookup(Configuration.class);
        
		String stealthModeString = configuration.getProperty(HippoProperty.STEALTH_MODE);
		stealthMode = Boolean.parseBoolean(stealthModeString);
		
		logger.debug("Stealth mode: " + stealthMode);
		
		requiresSignin = getStringSet(config, "requiresSignin");
		requiresSigninStealth = getStringSet(config, "requiresSigninStealth");
		noSignin = getStringSet(config, "noSignin");
		
		jspPages = new HashSet<String>();
		Set jspPaths = context.getResourcePaths("/jsp/");
		if (jspPaths != null) {
			for (Object o : jspPaths) {
				String path = (String)o;
				if (path.endsWith(".jsp") && path.indexOf('/') != -1)
					jspPages.add(path.substring(5, path.length() - 4));
			}
		}
		logger.debug("jsp pages are {}", jspPages);
		
		jsp2Pages = new HashSet<String>();
		Set jsp2Paths = context.getResourcePaths("/jsp2/");
		if (jsp2Paths != null) {
			for (Object o : jsp2Paths) {
				String path = (String)o;
				if (path.endsWith(".jsp") && path.indexOf('/') != -1)
					jsp2Pages.add(path.substring(6, path.length() - 4));
			}
		}
		logger.debug("jsp2 pages are {}", jsp2Pages);
		
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
		
		for (String p : jspPages) {
			if (!withSigninRequirements.contains(p)) {
				if (p.startsWith("psa-"))
					noSignin.add(p);
				else {
					// This warning is generated superfluously on some unused /jsp pages
					// for now
					logger.error(".jsp {} does not have its signin requirements specified", p);
				}
			}
		}

		for (String p : jsp2Pages) {
			if (!withSigninRequirements.contains(p)) {
				if (p.startsWith("psa-"))
					noSignin.add(p);
				else {
					logger.error(".jsp {} does not have its signin requirements specified", p);
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
			if (!jspPages.contains(p) && !htmlPages.contains(p) && !jsp2Pages.contains(p)) {
				logger.warn("Page '{}' in servlet config is not a .jsp or .html we know about", p);
			}
		}
		
		psaLinks = new ArrayList<String>();
		for (String p : noSignin) {
			if (p.startsWith("psa-"))
				psaLinks.add(p);
		}
		logger.debug("Added {} PSAs: {}", psaLinks.size(), psaLinks);		
        
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
	}
	
	@Override
	public void destroy() {
		stopBuildStampScan();
	}
}
