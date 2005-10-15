package com.dumbhippo.web;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AbstractEjbLink;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.LoginRequired;
import com.dumbhippo.web.LoginCookie.BadTastingException;

/**
 * All usage of InitialContext() and all logging in / authentication should go
 * through here. We don't want potentially misusable EJB interfaces all over the
 * web tier.
 * 
 * The EjbLink is tied to a particular user.
 * 
 * @author hp
 * 
 */
public class EjbLink extends AbstractEjbLink implements Serializable {

	private static final long serialVersionUID = 0L;

	static public class NotLoggedInException extends Exception {
		private static final long serialVersionUID = 0L;

		public NotLoggedInException(String string) {
			super(string);
		}
	}

	static private final Log logger = GlobalSetup.getLog(EjbLink.class);

	// if non-null, we are logged in
	private String personId;

	private Map<Class,Object> ejbCache;
	
	private Scope scope;
	
	private void init(Scope scope) {
		this.scope = scope;
		ejbCache = new HashMap<Class,Object>();		
	}
	
	public EjbLink() {
		init(Scope.NONE);
	}
	
	// only callable from getForSession()
	private EjbLink(HttpSession session) {
		init(Scope.SESSION);
		session.setAttribute(getKey(), this);
	}

	// only callable from getForApplication()
	private EjbLink(ServletContext context) {
		init(Scope.APPLICATION);
		context.setAttribute(getKey(), this);
	}
	
	private <T> T uncheckedNameLookup(Class<T> clazz) {
		try {
			return super.nameLookup(clazz);
		} catch (NamingException e) {
			e.printStackTrace();
			logger.error("Failed to look up interface " + clazz.getCanonicalName());
			logger.error(e);
			return null;
		}
	}
	/*
	 *  @see com.dumbhippo.server.AbstractEjbLink#nameLookup(java.lang.Class)
	 */
	@Override
	public <T> T nameLookup(Class<T> clazz) throws NamingException {
		throw new UnsupportedOperationException("Can't use nameLookup(), have to use getEjb()");
	}
	
	/**
	 * Gets an instance of a given EJB interface; always returns the same 
	 * instance for a given interface, unlike InitialContext.lookup(). 
	 * Automatically uses the LoginRequired interface if needed. Throws an
	 * unchecked exception if you try to 
	 * use an object that requires login, when you aren't logged in.
	 * This method should be fast and convenient so there's no point caching its
	 * result yourself.
	 * 
	 * @throws IllegalStateException if not logged in
	 */
	public synchronized <T> T getEjb(Class<T> clazz) {
		
		Object cached = ejbCache.get(clazz);
		if (cached != null) {
			return clazz.cast(cached);
		}
		
		if (clazz.isAnnotationPresent(BanFromWebTier.class)) {
			throw new IllegalArgumentException("Class " + clazz.getCanonicalName() + " is banned from the web tier");
		}
		
		T obj = uncheckedNameLookup(clazz);

		if (obj == null)
			return null;
		
		/*
		 for (Class i : obj.getClass().getInterfaces()) {
			 logger.info("  implements " + i.getCanonicalName());
			 for (Annotation a : i.getAnnotations()) {
				 logger.info("     with annotation " + a.getClass().getCanonicalName());
			 }
		 }
		 */
		 
		if (obj instanceof LoginRequired) {
			logger.info("  logging in object " + clazz.getCanonicalName());
			
			LoginRequired loginRequired = (LoginRequired) obj;
			
			if (personId == null) {
				throw new IllegalStateException("To use EJB " + clazz.getCanonicalName() + " you must log the user in"); 
			} else {
				loginRequired.setLoggedInUserId(personId);
			}
		} else {
			logger.info("  object does not need login " + clazz.getCanonicalName());
		}

		// create our own proxy, though since JBoss does this anyway it may be kind of 
		// pointless. For now it just checks the @BanFromWebTier annotation on methods
		// and removes the JBossProxy interface in effect.
		T proxy = (T) InterfaceFilterProxyFactory.newProxyInstance(obj, clazz);
		ejbCache.put(clazz, proxy);
		
		return proxy;
	}

	private void attemptLoginFromFacesContextLoggingErrors() {
		try {
			attemptLoginFromFacesContext();
		} catch (BadTastingException e) {
			logger.debug("Failed to login (bad cookie)", e);
		} catch (NotLoggedInException e) {
			logger.debug("Failed to login (not logged in)", e);
		}
	}
	
	/**
	 * Look for login cookie and find corresponding account; throw exception if
	 * login fails.
	 * @throws BadTastingException
	 * @throws NotLoggedInException
	 */
	public void attemptLoginFromFacesContext() throws BadTastingException, NotLoggedInException {
		
		// this is checked down in the lowest-level attemptLogin(), but 
		// doing it here also to save all that work
		if (this.personId != null)
			return;
		
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletRequest request = (HttpServletRequest) ctx.getRequest();

		if (request == null) {
			throw new IllegalStateException("No current HTTP request to get login cookie from");
		}
		
		attemptLogin(request);
	}
	
	/**
	 * Look for login cookie and find corresponding account; throw exception if
	 * login fails.
	 * 
	 * @param request
	 *            the http request
	 * @throws BadTastingException
	 * @throws NotLoggedInException
	 */
	public void attemptLogin(HttpServletRequest request) throws BadTastingException, NotLoggedInException {
		
		// this is checked down in the lowest-level attemptLogin(), but 
		// doing it here also to save all that work
		if (this.personId != null)
			return;
		
		LoginCookie loginCookie = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie c : cookies) {
				if (c.getName().equals(LoginCookie.COOKIE_NAME)) {
					loginCookie = new LoginCookie(c);
					break;
				}
			}
		}

		attemptLogin(loginCookie);
	}

	/**
	 * Try to login from a cookie.
	 * 
	 * @param loginCookie
	 *            the http cookie with the login info
	 * @throws BadTastingException
	 * @throws NotLoggedInException
	 */
	public void attemptLogin(LoginCookie loginCookie) throws BadTastingException, NotLoggedInException {

		if (loginCookie == null) {
			throw new NotLoggedInException("No login cookie set");
		}

		attemptLogin(loginCookie.getPersonId(), loginCookie.getAuthKey());
	}

	/**
	 * Try to login from a personId/authKey.
	 * 
	 * @param personId
	 *            the person ID
	 * @param authKey
	 *            the auth key
	 * @throws BadTastingException
	 * @throws NotLoggedInException
	 */
	public synchronized void attemptLogin(String personId, String authKey) throws BadTastingException, NotLoggedInException {

		if (scope == Scope.APPLICATION) {
			throw new IllegalStateException("You don't want to log someone in to an application-scope EjbLink");
		}
		
		if (this.personId != null)
			return; // already logged in
		
		AccountSystem accountSystem = uncheckedNameLookup(AccountSystem.class);
		
		HippoAccount account = accountSystem.lookupAccountByPersonId(personId);

		if (account == null) {
			throw new BadTastingException("Cookie had invalid person ID '" + personId + "'");
		}

		if (!account.checkClientCookie(authKey)) {
			throw new BadTastingException("Cookie had invalid or expired auth key in it '" + authKey + "'");
		}

		// OK !
		this.personId = personId;
	}

	public String getLoggedInUser() {
		return personId;
	}
	
	public boolean isLoggedIn() {
		return personId != null;
	}

	public boolean checkLoginFromFacesContext(Object object) {
		if (personId == null) {
			attemptLoginFromFacesContextLoggingErrors();
			
			// inject previously-missed objects, since we transitioned to logged in state
			if (personId != null) {
				inject(object);
			}
		}
		
		return personId != null;
	}
	
	static private void setField(Object object, Field field, Object value) {
		field.setAccessible(true);
		try {
			logger.debug("  Setting field " + field.toGenericString() +
					" of object " + object.getClass().getCanonicalName() + 
					" to value = " + value);
			field.set(object, value);
		} catch (IllegalArgumentException e) {
			logger.error(e);
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			logger.error(e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Finds any fields in the object with the Inject annotation with a scope
	 * matching this EjbLink, and fills in those fields. The fields must be an
	 * EJB interface or an EjbLink.
	 * 
	 * @param object object to be injected
	 * @returns true if anything was injectable
	 */
	public boolean inject(Object object) {
		boolean sawSomething = false;
		
		Field[] fields = object.getClass().getDeclaredFields();
		logger.debug("Injecting " + object.getClass().getCanonicalName() + 
				" with " + fields.length + " fields and EjbLink of scope " + scope);
		for (Field f : fields) {
			Inject inject = f.getAnnotation(Inject.class);
			if (inject != null && inject.value() == scope) {
				if (Modifier.isStatic(f.getModifiers())) {
					throw new RuntimeException("Inject annotation on static fields is not going to work");
				}
				
				sawSomething = true;
				
				Object oldValue;
				try {
					f.setAccessible(true);
					oldValue = f.get(object);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				
				// if we already injected, don't replace it
				if (oldValue == null) {
					if (f.getType() == EjbLink.class) {
						setField(object, f, this);
					} else {
						Class c = f.getType();
						if (personId == null &&
								LoginRequired.class.isAssignableFrom(c)) {
							logger.debug("  not injecting field " + f.toGenericString() + " since not logged in yet");
						} else {
							Object value = getEjb(c);
							setField(object, f, value);
						}
					}
				}
			} else {
				logger.debug("  Field " + f.toGenericString() + " does not need injecting");
			}
		}
		return sawSomething;
	}
	
	static private String getKey() {
		//	our class name is a nice namespaced key to use
		return EjbLink.class.getCanonicalName();
	}
	
	static EjbLink getForSession(HttpSession session) {
		EjbLink ejb;
		synchronized(session) {
			ejb = (EjbLink) session.getAttribute(getKey());
			if (ejb == null) {
				ejb = new EjbLink(session); // sets us on the session
			}
		}
		return ejb;
	}
	
	static EjbLink getForSessionFromFacesContext() {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpSession session = (HttpSession) ctx.getSession(/* createIfNotFound = */ false);
		return getForSession(session);
	}
	
	static EjbLink getForApplication(ServletContext context) {
		EjbLink ejb;
		synchronized(context) {
			ejb = (EjbLink) context.getAttribute(getKey());
			if (ejb == null) {
				ejb = new EjbLink(context); // sets us on the session
			}
		}
		return ejb;
	}
	
	static EjbLink getForApplicationFromFacesContext() {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		ServletContext servletContext = (ServletContext) ctx.getContext();
		return getForApplication(servletContext);
	}
	
	static void injectFromFacesContext(Object object, Scope... scopes) {
		for (Scope s : scopes) {
			EjbLink ejb = null;
			if (s == Scope.NONE) {
				ejb = new EjbLink();
			} else if (s == Scope.SESSION) {
				ejb = getForSessionFromFacesContext();
			} else if (s == Scope.APPLICATION) {
				ejb = getForApplicationFromFacesContext();
			}
			
			if (ejb == null)
				throw new RuntimeException("Could not get requested EjbLink for injection");

			if (ejb.scope != s)
				throw new RuntimeException("We created the wrong scope of EjbLink");
		
			if (ejb.scope != Scope.APPLICATION) {
				ejb.attemptLoginFromFacesContextLoggingErrors();
			}
			
			if (!ejb.inject(object)) {
				throw new IllegalStateException("Tried to inject scope " + s + " but object has no fields with that @Inject scope");
			}
		}
	}
}
