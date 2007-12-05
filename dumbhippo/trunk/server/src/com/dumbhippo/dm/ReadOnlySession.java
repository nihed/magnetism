package com.dumbhippo.dm;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.schema.FeedPropertyHolder;
import com.dumbhippo.dm.store.StoreKey;

/**
 * ReadOnlySession is a session used for reading from the database. It is an error to
 * write to the database in the same transaction as a ReadOnlySession. The separation
 * of sessions into read-only sessions and read-write sessions allows read-only sessions
 * to use cached data aggressively without worrying about invalidation or isolation
 * between different read-only sessions.
 *  
 * @author otaylor
 */
public class ReadOnlySession extends CachedSession {
	@SuppressWarnings("unused")
	private static Logger logger = GlobalSetup.getLogger(ReadOnlySession.class);
	private long txTimestamp;

	protected ReadOnlySession(DataModel model, DMClient client, DMViewpoint viewpoint) {
		super(model, client, viewpoint);
		this.txTimestamp = model.getTimestamp();
	}
	
	@Override
	public <K, T extends DMObject<K>> Object fetchAndFilter(StoreKey<K,T> key, int propertyIndex) throws NotCachedException {
		DMPropertyHolder<K,T,?> property = key.getClassHolder().getProperty(propertyIndex);
		if (!property.isCached())
			throw new NotCachedException();
			
		Object value = model.getStore().fetch(key, propertyIndex);

//		logger.debug("Found value for {}.{} in the cache", key, property.getName());

		return property.rehydrate(getViewpoint(), key.getKey(), value, this, !bypassFilter);
	}

	@Override
	public <K, T extends DMObject<K>> Object storeAndFilter(StoreKey<K,T> key, int propertyIndex, Object value) {
		DMPropertyHolder<K,T,?> property = key.getClassHolder().getProperty(propertyIndex);
		if (property.isCached()) {
			model.getStore().store(key, propertyIndex, property.dehydrate(value), txTimestamp);
//			logger.debug("Cached new value for {}.{}", key, property.getName());
		}

		if (bypassFilter)
			return value;
		else
			return property.filter(getViewpoint(), key.getKey(), value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <K, T extends DMObject<K>> DMFeed<?> createFeedWrapper(StoreKey<K, T> key, int propertyIndex, DMFeed<T> rawFeed) {
		FeedPropertyHolder<K, T, ?, ?> feedProperty = (FeedPropertyHolder<K, T, ?, ?>)key.getClassHolder().getProperty(propertyIndex);
		
		CachedFeed cached = model.getStore().getOrCreateCachedFeed(key, propertyIndex, txTimestamp);
		return new FeedWrapper(feedProperty, key.getKey(), rawFeed, cached);
	}

	@Override
	public void afterCompletion(int status) {
	}
}
