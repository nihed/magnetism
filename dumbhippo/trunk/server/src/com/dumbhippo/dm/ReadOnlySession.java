package com.dumbhippo.dm;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;


public class ReadOnlySession extends DMSession {
	static private Logger logger = GlobalSetup.getLogger(DMSession.class);

	protected ReadOnlySession(DMCache cache, DMViewpoint viewpoint) {
		super(cache, viewpoint);
	}
	
	public static ReadOnlySession getCurrent() {
		DMSession session = DMCache.getInstance().getCurrentSession();
		if (session instanceof ReadOnlySession)
			return (ReadOnlySession)session;
		
		throw new IllegalStateException("ReadOnlySession.getCurrent() called when not inside a ReadOnlySession");
	}
	
	public <K, T extends DMObject<K>> Object fetchAndFilter(Class<T> clazz, K key, String propertyName) throws NotCachedException {
		throw new NotCachedException();
	}

	public <K, T extends DMObject<K>> Object storeAndFilter(Class<T> clazz, K key, String propertyName, Object value) {
		logger.debug("Caching new value for {}#{}.{}", new Object[] { clazz.getSimpleName(), key, propertyName });
		return value;
	}
}
