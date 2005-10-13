package com.dumbhippo.server;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

public class AbstractEjbLink {

	private InitialContext namingContext;

	private void init(boolean verbose) {
		try {
			namingContext = new InitialContext();

			if (verbose) {
				NamingEnumeration names = namingContext.list("");
				while (names.hasMore()) {
					NameClassPair pair = (NameClassPair) names.next();

					System.err.println(String.format("Name '%s' bound to class '%s'", pair.getName(), pair
							.getClassName()));
				}
			}
		} catch (NamingException e) {
			e.printStackTrace();
			throw new RuntimeException("AbstractEjbLink did not get an InitialContext", e);
		}
	}

	protected AbstractEjbLink() {
		init(false);
	}

	protected AbstractEjbLink(boolean verbose) {
		init(verbose);
	}

	@SuppressWarnings("unchecked")
	protected <T> T nameLookup(Class<T> clazz) throws NamingException {
		if (clazz == null)
			throw new IllegalArgumentException("Class passed to nameLookup() is null");
		
		String name = clazz.getCanonicalName();		
		// System.out.println("Looking up '" + name + "'");
		if (!clazz.isInterface())
			throw new IllegalArgumentException("Class passed to nameLookup() has to be an interface, not " + name);
		return (T) namingContext.lookup(name);
	}
}
