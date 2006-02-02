package com.dumbhippo.server.util;

import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.NonUniqueObjectException;
import org.hibernate.exception.ConstraintViolationException;

import com.dumbhippo.StringUtils;

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
		InitialContext namingContext; // note, if ever caching this, it isn't threadsafe
		namingContext = new InitialContext();
		if (clazz == null)
			throw new IllegalArgumentException("Class passed to nameLookup() is null");
		
		String name = clazz.getCanonicalName();		
		if (!clazz.isInterface())
			throw new IllegalArgumentException("Class passed to nameLookup() has to be an interface, not " + name);

		return clazz.cast(namingContext.lookup(name));
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
}
