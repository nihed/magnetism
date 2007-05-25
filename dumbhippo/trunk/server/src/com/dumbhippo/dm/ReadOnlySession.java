package com.dumbhippo.dm;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.store.StoreKey;


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
	public <K, T extends DMObject<K>> Object fetchAndFilter(StoreKey<K,T> key, int propertyIndex) throws NotCachedException {
		Object value = model.getStore().fetch(key, propertyIndex);

		DMPropertyHolder property = key.getClassHolder().getProperty(propertyIndex);
		logger.debug("Found value for {}.{} in the cache", key, property.getName());

		return property.rehydrate(value, this);
	}

	@Override
	public <K, T extends DMObject<K>> Object storeAndFilter(StoreKey<K,T> key, int propertyIndex, Object value) {
		DMPropertyHolder property = key.getClassHolder().getProperty(propertyIndex);
		model.getStore().store(key, propertyIndex, property.dehydrate(value), txTimestamp);
		
		logger.debug("Cached new value for {}.{}", key, property.getName());
		
		return value;
	}
	
	@Override
	public void afterCompletion(int status) {
	}
}
