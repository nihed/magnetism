package com.dumbhippo.dm;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;


public class ReadOnlySession extends DMSession {
	static private Logger logger = GlobalSetup.getLogger(DMSession.class);

	protected ReadOnlySession(DataModel model, DMViewpoint viewpoint) {
		super(model, viewpoint);
	}
	
	public static ReadOnlySession getCurrent() {
		DMSession session = DataModel.getInstance().getCurrentSession();
		if (session instanceof ReadOnlySession)
			return (ReadOnlySession)session;
		
		throw new IllegalStateException("ReadOnlySession.getCurrent() called when not inside a ReadOnlySession");
	}
	
	public <K, T extends DMObject<K>> Object fetchAndFilter(Class<T> clazz, K key, String propertyName) throws NotCachedException {
		// Ordering here increases effiicency in not-cached case
		Object value = model.fetchFromCache(clazz, key, propertyName);
		DMPropertyHolder property = model.getDMClass(clazz).getProperty(propertyName);
		
		logger.debug("Found value for {}#{}.{} in the cache", new Object[] { clazz.getSimpleName(), key, propertyName });

		return property.rehydrate(value, this);
	}

	public <K, T extends DMObject<K>> Object storeAndFilter(Class<T> clazz, K key, String propertyName, Object value) {
		logger.debug("Caching new value for {}#{}.{}", new Object[] { clazz.getSimpleName(), key, propertyName });
		
		DMPropertyHolder property = model.getDMClass(clazz).getProperty(propertyName);
		model.storeInCache(clazz, key, propertyName, property.dehydrate(value));
		
		return value;
	}
}
