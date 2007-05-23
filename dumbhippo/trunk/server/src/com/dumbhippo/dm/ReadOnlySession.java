package com.dumbhippo.dm;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;


public class ReadOnlySession extends DMSession {
	private static Logger logger = GlobalSetup.getLogger(ReadOnlySession.class);
	private long txTimestamp;

	protected ReadOnlySession(DataModel model, DMViewpoint viewpoint) {
		super(model, viewpoint);
		this.txTimestamp = model.getTimestamp();
	}
	
	public static ReadOnlySession getCurrent() {
		DMSession session = DataModel.getInstance().getCurrentSession();
		if (session instanceof ReadOnlySession)
			return (ReadOnlySession)session;
		
		throw new IllegalStateException("ReadOnlySession.getCurrent() called when not inside a ReadOnlySession");
	}
	
	@Override
	public <K, T extends DMObject<K>> Object fetchAndFilter(Class<T> clazz, K key, int propertyIndex) throws NotCachedException {
		DMClassHolder classHolder = model.getDMClass(clazz);
		Object value = model.getStore().fetch(classHolder, key, propertyIndex);
		DMPropertyHolder property = classHolder.getProperty(propertyIndex);
		
		logger.debug("Found value for {}#{}.{} in the cache", new Object[] { clazz.getSimpleName(), key, property.getName() });

		return property.rehydrate(value, this);
	}

	@Override
	public <K, T extends DMObject<K>> Object storeAndFilter(Class<T> clazz, K key, int propertyIndex, Object value) {
		DMClassHolder classHolder = model.getDMClass(clazz);
		DMPropertyHolder property = classHolder.getProperty(propertyIndex);
		model.getStore().store(classHolder, key, propertyIndex, property.dehydrate(value), txTimestamp);
		
		logger.debug("Cached new value for {}#{}.{}", new Object[] { clazz.getSimpleName(), key, property.getName() });
		
		return value;
	}
	
	@Override
	public void afterCompletion(int status) {
	}
}
