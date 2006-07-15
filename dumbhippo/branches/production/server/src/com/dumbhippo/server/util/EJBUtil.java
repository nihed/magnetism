package com.dumbhippo.server.util;

import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;

import org.hibernate.NonUniqueObjectException;
import org.hibernate.exception.ConstraintViolationException;

import com.dumbhippo.StringUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.server.NotFoundException;

public class EJBUtil {
	
	private static final int MAX_SEARCH_TERMS = 3;
	// we avoid using \ because by the 
	// time you go through Java and MySQL also interpreting it
	// it becomes really confusing
	private static final char LIKE_ESCAPE_CHAR = '^';
	
	/**
	 * Very simple wrapper around InitialContext.lookup that looks up an
	 * a desired session bean in JNDI by the interface name of its primary 
	 * interface. 
	 * 
	 * If I understand correctly, the fact that this works is relying on JBoss 
	 * specifics. Thereis no standard way of looking up bean at runtime without 
	 * having specified a dependency on the bean via an @EJB annotation (On a field or 
	 * on a class.) We are relying here on the fact that JBoss (in the absence
	 * of its extension @LocalBinding annotation) uses the fully qualified
	 * name of the bean's interface as the bean's JNDI name.
	 *    
	 * @param clazz the class of the (local or remote) EJB interface to look up.
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
		
		String name = clazz.getCanonicalName();		
		if (!clazz.isInterface())
			throw new IllegalArgumentException("Class passed to nameLookup() has to be an interface, not " + name);

		return clazz.cast(uncheckedDynamicLookup(name));
	}
	
	public static Object uncheckedDynamicLookup(String name) throws NamingException {
		InitialContext namingContext; // note, if ever caching this, it isn't threadsafe
		namingContext = new InitialContext();	
		return namingContext.lookup(name);
	}
	
	public static <T> T contextLookup(EJBContext ejbContext, Class<T> clazz) {
		if (clazz == null)
			throw new IllegalArgumentException("Class passed to contextLookup() is null");
		
		String name = clazz.getCanonicalName();		
		if (!clazz.isInterface())
			throw new IllegalArgumentException("Class passed to contextLookup() has to be an interface, not " + name);

		return clazz.cast(ejbContext.lookup(name));
	}
	
	// Returns true if this is an exception we would get with a race condition
	// between two people trying to create the same object at once
	public static boolean isDuplicateException(Exception e) {
		return ((e instanceof EJBException &&
				 ((EJBException)e).getCausedByException() instanceof ConstraintViolationException) ||
	            e instanceof NonUniqueObjectException);
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
}
