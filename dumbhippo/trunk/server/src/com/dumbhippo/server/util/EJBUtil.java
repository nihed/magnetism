package com.dumbhippo.server.util;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.ejb.EJBContext;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.JDBCException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.ejb3.entity.HibernateSession;
import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;

public class EJBUtil {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(EJBUtil.class);	
	
	private static final String ROOT_NAME = "dumbhippo";
	private static final String CLASS_PREFIX = ROOT_NAME + "/";
	
	private static final int MAX_SEARCH_TERMS = 3;
	// we avoid using \ because by the 
	// time you go through Java and MySQL also interpreting it
	// it becomes really confusing
	private static final char LIKE_ESCAPE_CHAR = '^';
	
	public static Context getHAContext() throws NamingException {
		Configuration config = defaultLookup(Configuration.class);
		Properties p = new Properties();
		p.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
		p.put(Context.URL_PKG_PREFIXES, "jboss.naming:org.jnp.interfaces");
		String bind;
		String port;
		try {
			bind = System.getProperty("jboss.bind.address", "localhost");
			port = config.getProperty("dumbhippo.server.hajndiPort");
		} catch (PropertyNotFoundException e) {
			bind = "localhost";
			port = "1100";
		}
		p.put(Context.PROVIDER_URL, bind + ":" + port);
		return new InitialContext(p);
	}
	
	public static <T> T defaultHALookup(Class<T> clazz) {
		try {
			return defaultHALookupChecked(clazz);
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
	}	
	
	/**
	 * Very simple wrapper around InitialContext.lookup that looks up an
	 * a desired local session bean in JNDI by the interface name of its primary 
	 * interface. 
	 * 
	 * If I understand correctly, the fact that this works is relying on JBoss 
	 * specifics. There is no standard way of looking up bean at runtime without 
	 * having specified a dependency on the bean via an @EJB annotation (On a field or 
	 * on a class.) We are relying here on the fact that JBoss (in the absence
	 * of its extension @LocalBinding annotation) uses the fully qualified
	 * name of a local bean's interface as the bean's JNDI name, suffixed with "/local".
	 *    
	 * @param clazz the class of the local EJB interface to look up.
	 * @return the found EJB
	 */
	public static <T> T defaultLookup(Class<T> clazz) {
		try {
			return defaultLookupChecked(clazz);
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** 
	 * Like defaultLookup, but for remote interfaces.
	 * 
	 * @param clazz the class of the remote EJB interface to look up.
	 * @return the found EJB
	 */
	public static <T> T defaultLookupRemote(Class<T> clazz) {
		try {
			return defaultLookupRemoteChecked(clazz);
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Like defaultLookupRemote() but throws a checked NamingException. You should
	 * use this function only if you have useful recovery to do if lookup fails.
	 * 
	 * @param clazz the class of the remote EJB interface to look up.
	 * @return the found EJB
	 * @throws NamingException if an error occurs finding the object; this might
	 *   be because of a network error, or becaus the server providing the object
	 *   doesn't exist.
	 */
	public static <T> T defaultLookupRemoteChecked(Class<T> clazz) throws NamingException {

		if (clazz == null)
			throw new IllegalArgumentException("Class passed to nameLookup() is null");
		
		String name = clazz.getSimpleName();
		if (!clazz.isInterface())
			throw new IllegalArgumentException("Class passed to nameLookup() has to be an interface, not " + name);

		return clazz.cast(uncheckedDynamicLookupRemote(name));
	}
	
	/**
	 * Like defaultLookup() but throws a checked NamingException. You should
	 * use this function only if you have useful recovery to do if lookup fails.
	 * 
	 * @param clazz the class of the (local or remote) EJB interface to look up.
	 * @return the found EJB
	 * @throws NamingException if an error occurs finding the object; this might
	 *   be because of a network error, or becaus the server providing the object
	 *   doesn't exist.
	 */
	public static <T> T defaultLookupChecked(Class<T> clazz) throws NamingException {

		if (clazz == null)
			throw new IllegalArgumentException("Class passed to nameLookup() is null");
		
		String name = clazz.getSimpleName();		
		if (!clazz.isInterface())
			throw new IllegalArgumentException("Class passed to nameLookup() has to be an interface, not " + name);
		if (clazz.isAnnotationPresent(Local.class))		
			return clazz.cast(uncheckedDynamicLookupLocal(name));
		else
			return clazz.cast(uncheckedDynamicLookupRemote(name));
	}	
	
	public static <T> T defaultHALookupChecked(Class<T> clazz) throws NamingException {

		if (clazz == null)
			throw new IllegalArgumentException("Class passed to nameLookup() is nNull");
		
		String name = clazz.getSimpleName();		
		if (!clazz.isInterface())
			throw new IllegalArgumentException("Class passed to nameLookup() has to be an interface, not " + name);
		if (clazz.isAnnotationPresent(Local.class))		
			return clazz.cast(uncheckedDynamicLookupLocal(name, true));
		else
			return clazz.cast(uncheckedDynamicLookupRemote(name, true));
	}	
	
	public static Object uncheckedDynamicLookupLocal(String name) throws NamingException {	
		return uncheckedDynamicLookupLocal(name, false);
	}
	
	public static Object uncheckedDynamicLookupLocal(String name, boolean ha) throws NamingException {	
		return uncheckedDynamicLookup(CLASS_PREFIX + name + "Bean/local", ha);
	}	
	
	public static Object uncheckedDynamicLookupRemote(String name) throws NamingException {
		return uncheckedDynamicLookupRemote(name, false);
	}
	
	public static Object uncheckedDynamicLookupRemote(String name, boolean ha) throws NamingException {
		if (name.endsWith("Remote"))
			name = name.substring(0, name.lastIndexOf("Remote"));
		name = name + "Bean";
		return uncheckedDynamicLookup(CLASS_PREFIX + name + "/remote", ha);
	}		
	
	public static Object uncheckedDynamicLookup(String name) throws NamingException {
		return uncheckedDynamicLookup(name, false);
	}	
	
	public static Object uncheckedDynamicLookup(String name, boolean ha) throws NamingException {
		Context namingContext; // note, if ever caching this, it isn't threadsafe
		namingContext = ha ? getHAContext() : new InitialContext();	
		return namingContext.lookup(name);
	}
	
	public static <T> T contextLookup(EJBContext ejbContext, Class<T> clazz) {
		if (clazz == null)
			throw new IllegalArgumentException("Class passed to contextLookup() is null");
		
		String name = clazz.getSimpleName();		
		if (!clazz.isInterface())
			throw new IllegalArgumentException("Class passed to contextLookup() has to be an interface, not " + name);
		String suffix = clazz.isAnnotationPresent(Local.class) ? "local" : "remote";	
		name = name + "Bean";
		return clazz.cast(ejbContext.lookup(CLASS_PREFIX + name + "/" + suffix));
	} 
	
	@SuppressWarnings("unused")
	private static void dumpContext(String name) {
		try {
			logger.debug("Dumping items in {}", name);
			Context namingContext = new InitialContext();
			NamingEnumeration ne = namingContext.list(name);
			while (ne.hasMore()) {
				Object o = ne.next();
				logger.debug(" listed object {} {}", o.getClass().getName(), o);
				try {
					if (o instanceof NameClassPair) {
						NameClassPair ncp = (NameClassPair) o;
						
						String n1 = null, n2 = null, n3 = null;
						boolean relative = false;
						try {
							n1 = ncp.getName();
						} catch (UnsupportedOperationException e) {
							
						}
						try {
							n2 = ncp.getNameInNamespace();
						} catch (UnsupportedOperationException e) {
							
						}
						try {
							n3 = ncp.getClassName();
						} catch (UnsupportedOperationException e) {
							
						}
						try {
							relative = ncp.isRelative();
						} catch (UnsupportedOperationException e) {
							
						}
						
						logger.debug(" name={} nameInNamespace={} className={} relative={} ",
								new Object[] { n1, n2, n3, relative });
					}
				} catch (RuntimeException e) {
					logger.debug(" (failed to print NameClassPair): {}", e.getMessage());
				}
			}
			logger.debug("Done");
		} catch (NamingException e) {
			logger.debug("Failed to dump {}: {}", name, e.getMessage());
		}
	}
	

	static private final String[] beanIfacePackages = {
		"com.dumbhippo.server",
		"com.dumbhippo.server.blocks",
		"com.dumbhippo.live",
		"com.dumbhippo.search",
		"com.dumbhippo.services.caches"
	};
	
	// this is a huge hack
	static public Class<?> loadLocalBeanInterface(ClassLoader loader, String name) {
		Class<?> klass = null;

		if (!name.startsWith(CLASS_PREFIX)) {
			logger.warn("Name {} does not start with {} as expected",
					name, CLASS_PREFIX);
			return null;
		}
		if (!name.endsWith("Bean/local")) {
			logger.warn("Name {} does not end with Bean/local as expected",
					name);
			return null;
		}
		
		String ifaceWithoutPackage = name.substring(CLASS_PREFIX.length(),
				name.length() - "Bean/local".length());
		
		for (String pkg : beanIfacePackages) {
			try {
				String fullName = pkg + "." + ifaceWithoutPackage;
				//logger.debug(" trying to load {}", fullName);
				klass = loader.loadClass(fullName);
				break;
			} catch (ClassNotFoundException e) {
			}
		}
		
		if (klass == null) {
			logger.warn("Bean {} interface {} not found in any package or not loadable", name, ifaceWithoutPackage);
			return null;
		} else {
			if (!klass.isAnnotationPresent(Local.class)) {
				if (!klass.isAnnotationPresent(Remote.class))
					logger.warn("Interface {} does not have @Local or @Remote annotation", klass.getName());
				return null;
			}
			return klass;
		}
	}
	
	public static Collection<String> listLocalBeanNames() throws NamingException {
		/*dumpContext("");
		dumpContext(rootName);
		dumpContext(classPrefix + "IdentitySpiderBean");
		dumpContext(classPrefix + "IdentitySpiderBean/local");
		dumpContext(classPrefix + "BlogBlockHandlerBean");
		dumpContext(classPrefix + "BlogBlockHandlerBean/local");*/		
		//logger.debug("Listing known bean classes");
		Set<String> beanClasses = new HashSet<String>();
		Context namingContext = new InitialContext();
		NamingEnumeration ne = namingContext.list(ROOT_NAME);
		while (ne.hasMore()) {
			NameClassPair ncp = (NameClassPair) ne.next();
			//logger.debug(" bean '{}'", ncp.getName());
			beanClasses.add(CLASS_PREFIX + ncp.getName() + "/local");
		}
		//logger.debug("Done listing");
		return beanClasses;
	}
	
	public static Set<Class<?>> getConstraintViolationExceptions() {
		HashSet<Class<?>> exceptions = new HashSet<Class<?>>();
		exceptions.add(ConstraintViolationException.class);
		exceptions.add(NonUniqueObjectException.class);		
		return exceptions;
	}

	// Returns an exception we would get with a race condition
	// between two transactions trying to create the same object at once
	public static Set<Class<?>> getDuplicateEntryExceptions() {
		HashSet<Class<?>> exceptions = new HashSet<Class<?>>();
		exceptions.add(EntityExistsException.class);
		return exceptions;
	}
	
	public static boolean isDatabaseException(Exception e) {
		Throwable root = ExceptionUtils.getRootCause(e);
		// FIXME not sure what should really be here or if this will work
		if (root instanceof JDBCException)
			return true;
		else if (root instanceof SQLException)
			return true;
		else
			return false;
	}
	
	public static String likeClauseFromUserSearch(String userSearch, String... fields) {
		String[] terms = StringUtils.splitWords(userSearch);
		
		if (terms.length == 0)
			return null;
		
		StringBuilder clause = new StringBuilder("(");
		
		// MAX_SEARCH_TERMS is so people don't destroy our database, 
		// since our search technology is so high-tech
		for (int i = 0; i < terms.length && i < MAX_SEARCH_TERMS; ++i) {
			if (i > 0)
				clause.append(" OR ");
			for (int j = 0; j < fields.length; ++j) {
				if (j > 0)
					clause.append(" OR ");
				clause.append(fields[j] + " LIKE '%");
				for (char c : terms[i].toCharArray()) {
					if (c == '%') {
						clause.append(LIKE_ESCAPE_CHAR + "%");
					} else if (c == '_') {
						clause.append(LIKE_ESCAPE_CHAR + "_");
					} else if (c == LIKE_ESCAPE_CHAR) {
						// just eat it; EJB QL spec doesn't 
						// say there's any way to include the escape char
					} else if (c == '\'') {
						clause.append("''");
					} else {
						clause.append(c);
					}
				}
				clause.append("%'");
			}
		}
		// set escape character
		clause.append(" ESCAPE '" + LIKE_ESCAPE_CHAR + "')");
		
		String ret = clause.toString();
		
		return ret;
	}
	
	/**
	 * Look up a GuidPersistable in the database, checking its type.  
	 * @param em
	 * @param klass
	 * @param id
	 * @return
	 * @throws NotFoundException if the GuidPersistable doesn't exist or has another type
	 */
	public static <T extends GuidPersistable> T lookupGuid(EntityManager em, Class<T> klass, Guid id) throws NotFoundException {
		// we pass GuidPersistable.class to em.find() since it fails kind of 
		// opaquely if the type is wrong, and we would rather fail in the 
		// same way we would if we eventually move everything to EmbeddedGuidPersistable,
		// i.e. just get a "not found" 
		// this matters right now if e.g. we want to try loading a chat as a Post and 
		// a Group chat both and see which one works.

		GuidPersistable obj = em.find(GuidPersistable.class, id.toString());
		if (obj == null)
			throw new GuidNotFoundException(id);
		try {
			return klass.cast(obj);
		} catch (ClassCastException e) {
			throw new GuidNotFoundException(id, e);
		}
	}
	
	public static <T extends GuidPersistable> T lookupGuidString(EntityManager em, Class<T> klass, String id) throws ParseException, NotFoundException {
		Guid guid = new Guid(id); //throw Parse here instead of GuidNotFound if invalid
		
		return lookupGuid(em, klass, guid);
	}
	
	/**
	 * This function provides a workaround for
	 *  
	 *  http://opensource.atlassian.com/projects/hibernate/browse/HHH-2192
	 * 
	 * You should call it if you are updating a persistent collection without
	 * reading it first. Really, there is no advantage to this function of
	 * just calling collection.size(), but doing it this way marks the
	 * usage as a workaround, which is useful for future maintentance.
	 *  
	 * @param collection
	 */
	public static void forceInitialization(Collection collection) {
		if (collection instanceof PersistentCollection) {
			((PersistentCollection)collection).forceInitialization();
		}
	}
	
	/**
	 * For each affected entity class, 1) evict it from first-level cache and 2) assert that it's not in the second-level cache.
	 * @param em
	 * @param affectedEntityClasses
	 */
	public static void prepareUpdate(EntityManager em, Class<?>... affectedEntityClasses) {
		if (affectedEntityClasses.length == 0)
			throw new RuntimeException("You need to specify the entity classes involved in the update statement");
		Session session = ((HibernateSession) em).getHibernateSession();
		for (Class<?> klass : affectedEntityClasses) {
			if (klass.isAnnotationPresent(Cache.class))
				throw new RuntimeException("Entity class " + klass.getName() + " has second-level Cache annotation, so an UPDATE is a bad idea");
			
			// We want to throw all objects with the class out of the first level cache. 
			// Unfortunately, the evict() method on Session takes just one object, not a class.
			// So instead we sync to the db - flush() - and then just dump the whole first-level cache - clear()
			// SessionFactory has the method we want (evict-by-entity-name) for the second-level cache, but not useful here.
			
			session.flush();
			session.clear();
		}
	}

	public static String transactionStatusString(int status) {
		switch (status) {
		case Status.STATUS_ACTIVE:
			return "ACTIVE-" + status;
		case Status.STATUS_COMMITTED:
			return "COMMITTED-" + status;
		case Status.STATUS_MARKED_ROLLBACK:
			return "MARKED_ROLLBACK-" + status;
		case Status.STATUS_NO_TRANSACTION:
			return "NO_TRANSACTION-" + status;
		case Status.STATUS_PREPARED:
			return "PREPARED-" + status;
		case Status.STATUS_PREPARING:
			return "PREPARING-" + status;
		case Status.STATUS_ROLLEDBACK:
			return "ROLLEDBACK-" + status;
		case Status.STATUS_ROLLING_BACK:
			return "ROLLING_BACK-" + status;
		case Status.STATUS_UNKNOWN:
			return "UNKNOWN-" + status;
		default:
			return "NOT_HANDLED-" + status;
		}
	}	
	
	public static void assertTransactionStatus(int desired) {
		TransactionManager tm;
		try {
			tm = (TransactionManager) (new InitialContext()).lookup("java:/TransactionManager");
		} catch (NamingException e) {
			throw new RuntimeException("no TransactionManager found", e);
		}

		int txStatus;
		try {
			txStatus = tm.getStatus();
		} catch (SystemException e) {
			throw new RuntimeException("failed to get tx status", e);
		}
		
		if (txStatus != desired) {
			// Just warn instead of throw until we figure this out
			logger.warn("Unexpected tx status {} while expecting {}", transactionStatusString(txStatus),
					transactionStatusString(desired));
			//throw new RuntimeException("Unexpected tx status " + transactionStatusString(txStatus) + 
			//		" expecting " + transactionStatusString(desired));
		}
	}
	
	public static void assertNoTransaction() {
		assertTransactionStatus(Status.STATUS_NO_TRANSACTION);
	}
	
	public static void assertHaveTransaction() {
		assertTransactionStatus(Status.STATUS_ACTIVE);
	}
}
