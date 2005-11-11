package com.dumbhippo.server.util;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.NonUniqueObjectException;
import org.hibernate.exception.ConstraintViolationException;

/**
 * Very simple wrapper around InitialContext.lookup.
 */
public class EJBUtil {
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
}
