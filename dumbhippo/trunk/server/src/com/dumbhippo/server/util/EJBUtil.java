package com.dumbhippo.server.util;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Very simple wrapper around InitialContext.lookup.
 */
public class EJBUtil {
	public static <T> T defaultLookup(Class<T> clazz) {
		InitialContext namingContext;
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
}
