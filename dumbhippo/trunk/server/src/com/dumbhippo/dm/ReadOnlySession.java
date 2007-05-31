package com.dumbhippo.dm;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.store.StoreKey;


public class ReadOnlySession extends CachedSession {
	private static Logger logger = GlobalSetup.getLogger(ReadOnlySession.class);
	private long txTimestamp;

	protected ReadOnlySession(DataModel model, DMClient client, DMViewpoint viewpoint) {
		super(model, client, viewpoint);
		this.txTimestamp = model.getTimestamp();
	}
	
	@Override
	public <K, T extends DMObject<K>> Object fetchAndFilter(StoreKey<K,T> key, int propertyIndex) throws NotCachedException {
		DMPropertyHolder<K,T,?> property = key.getClassHolder().getProperty(propertyIndex);
		Object value = model.getStore().fetch(key, propertyIndex);

		logger.debug("Found value for {}.{} in the cache", key, property.getName());

		return property.rehydrate(getViewpoint(), key.getKey(), value, this);
	}

	@Override
	public <K, T extends DMObject<K>> Object storeAndFilter(StoreKey<K,T> key, int propertyIndex, Object value) {
		DMPropertyHolder<K,T,?> property = key.getClassHolder().getProperty(propertyIndex);
		model.getStore().store(key, propertyIndex, property.dehydrate(value), txTimestamp);
		
		logger.debug("Cached new value for {}.{}", key, property.getName());
		
		return property.filter(getViewpoint(), key.getKey(), value);
	}
	
	@Override
	public void afterCompletion(int status) {
	}
}
