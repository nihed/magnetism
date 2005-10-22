package com.dumbhippo.web;

import javax.servlet.http.HttpSession;

public class SessionUtil {
	
	/**
	 * Create an object on the session that must be a singleton.
	 * 
	 * @param <T>
	 * @param session
	 * @param klass
	 * @return
	 */
	public static <T> T findOrCreateSessionSingleton(HttpSession session, Class<T> klass) {
		synchronized (session) {
			T singleton = lookupSessionSingleton(session, klass);
			if (singleton == null) {
				singleton = WebEJBUtil.defaultLookup(klass);
				session.setAttribute(klass.getCanonicalName(), singleton);				
			}
			return singleton;
		}
	}
	
	public static <T> T lookupSessionSingleton(HttpSession session, Class<T> klass) {
		if (session == null)
			return null;
		Object obj = session.getAttribute(klass.getCanonicalName());
		if (obj == null)
			return null;
		return klass.cast(obj);
	}
}
