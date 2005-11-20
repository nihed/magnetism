package com.dumbhippo.server.util;

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
	 * Very simple wrapper around InitialContext.lookup.
	 */
	public static <T> T defaultLookup(Class<T> clazz) {
		InitialContext namingContext; // note, if ever caching this, it isn't threadsafe
		try {
			namingContext = new InitialContext();
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
		if (clazz == null)
			throw new IllegalArgumentException("Class passed to nameLookup() is null");
		
		String name = clazz.getCanonicalName();		
		if (!clazz.isInterface())
			throw new IllegalArgumentException("Class passed to nameLookup() has to be an interface, not " + name);

		try {
			return clazz.cast(namingContext.lookup(name));
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
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
