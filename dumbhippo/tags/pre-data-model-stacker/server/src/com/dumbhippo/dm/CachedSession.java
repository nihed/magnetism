package com.dumbhippo.dm;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.filter.CompiledFilter;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.server.NotFoundException;

/**
 * CachedSession is an abstract base class for the DMSession subclasses that keep
 * around a session-local cache of DMObjects.
 * 
 * FIXME: There are no other subclasses to DMSession any more; so either DMSession
 *  should be made into an interface, and this into the implementation, or this
 *  and DMSession should just be merged.
 * 
 * @author otaylor
 */
public abstract class CachedSession extends DMSession {
	@SuppressWarnings("unused")
	private static Logger logger = GlobalSetup.getLogger(CachedSession.class);
	private DMClient client;
	private DMViewpoint viewpoint;

	private Map<StoreKey<?,?>, DMObject<?>> sessionDMOs = new HashMap<StoreKey<?,?>, DMObject<?>>();
	
	// Set true when we are running with a temporary viewpoint ("su"); in that case
	// we must bypass the cache to avoid cross-pollution with the parent viewpoint
	private boolean bypassCache;
	
	// Set true when fetching a raw property value
	protected boolean bypassFilter;
	
	protected CachedSession(DataModel model, DMClient client, DMViewpoint viewpoint) {
		super(model);
		this.client = client;
		this.viewpoint = viewpoint;
		viewpoint.setSession(this);
	}

	@Override
	public DMClient getClient() {
		return client;
	}
	
	@Override
	public DMViewpoint getViewpoint() {
		return viewpoint;
	}
	
	private <K, T extends DMObject<K>> T findRawObject(StoreKey<K,T> storeKey) {
		if (bypassCache)
			return null;
		
		@SuppressWarnings("unchecked")
		T raw = (T)sessionDMOs.get(storeKey);
		
		return raw;
	}
	
	private <K, T extends DMObject<K>> T createRawObject(StoreKey<K,T> storeKey) {
		// logger.debug("Didn't find object for key {}, creating a new one", storeKey.getKey());
		return storeKey.getClassHolder().createInstance(storeKey.getKey(), this);
	}

	private <K, T extends DMObject<K>> T filterRawObject(StoreKey<K,T> storeKey, T raw) {
		if (bypassFilter)
			return raw;
		
		CompiledFilter<K,T> filter = storeKey.getClassHolder().getFilter();
		if (filter != null) {
			return filter.filterObject(getViewpoint(), raw);
		} else {
			return raw;
		}
	}
	
	@Override
	public <K, T extends DMObject<K>> T findUnchecked(StoreKey<K,T> storeKey) {
		T raw = findRawObject(storeKey);
		if (raw != null)
			return raw;
		
		raw = createRawObject(storeKey);
		
		if (!bypassCache) {
			// Despite the fact that we aren't supposed to return filtered results,
			// we have to filter before storing into the local cache (or alternatively, we'd
			// have to filter before returning a result from the local cache)
			
			T filtered = filterRawObject(storeKey, raw);
			if (filtered != null)
				sessionDMOs.put(storeKey, filtered);
		}
		
		return raw;
	}
	
	@Override
	public <K, T extends DMObject<K>> T find(StoreKey<K,T> storeKey) throws NotFoundException {
		T raw = findRawObject(storeKey);
		if (raw != null)
			return raw;
		
		raw = createRawObject(storeKey);
		ensureExists(storeKey, raw);
		
		T filtered = filterRawObject(storeKey, raw);
		if (filtered == null)
			throw new NotFoundException("No such object");

		if (!bypassCache)
			sessionDMOs.put(storeKey, filtered);
		
		return filtered;
	}

	/**
	 * Temporarily sets a new viewpoint as the viewpoint of the session while running
	 * the given code. While the new viewpoint is set, the session's cache of DMOs
	 * will not be read from or stored into, preventing leakage between the two
	 * viewpoints. (You should not access DMO's from the session inside the 
	 * runnable or you'll defeat this isolation; look them up again.)
	 * 
	 * If the session has a client (and thus will trigger notifications), you should not 
	 * run fetch operations while the new viewpoint is set, since the data returned
	 * will be incoherent with future notifications that are registered by the fetch. 
	 * Generally, this operation is expected to be most useful to perform a few
	 * privileged actions in the scope of a longer operation. 
	 * 
	 * @param newViewpoint
	 * @param runnable
	 */
	private void runWithViewpoint(DMViewpoint newViewpoint, Runnable runnable) {
		DMClient oldClient = client;
		DMViewpoint oldViewpoint = viewpoint;
		boolean oldBypassCache = bypassCache;
		
		try {
			client = null;
			viewpoint = newViewpoint;
			bypassCache = true;
			
			runnable.run();
			
		} finally {
			client = oldClient;
			viewpoint = oldViewpoint;
			bypassCache = oldBypassCache;
		}
	}
	
	/**
	 * Temporarily sets the system viewpoint as the viewpoint of the session while running
	 * the given code. See {@link #runWithViewpoint(DMViewpoint, Runnable)} for details
	 * and cautions.
	 * 
	 * @param newViewpoint
	 * @param runnable
	 */
	public void runAsSystem(Runnable runnable) {
		runWithViewpoint(model.getSystemViewpoint(), runnable);
	}

	@Override
	public <K, T extends DMObject<K>> Object getRawProperty(Class<T> clazz, K key, String propertyName) throws NotFoundException {
		StoreKey<K,T> storeKey = makeStoreKey(clazz, key);
		
		int propertyIndex = storeKey.getClassHolder().getPropertyIndex(propertyName);
		if (propertyIndex < 0)
			throw new RuntimeException("Class " + clazz.getName() + " has no property " + propertyName);
		
		DMPropertyHolder<?,? extends DMObject<?>,?> propertyHolder = storeKey.getClassHolder().getProperty(propertyIndex);

		// We look first in the cache, and only if that fails do we go ahead and create an 
		// object. The object we create is created from the special "unfiltered session",
		// which does no filtering and no caching.
		try {
			if (propertyHolder.isCached())
				return model.getStore().fetch(storeKey, propertyIndex);
		} catch (NotCachedException e) {
			/* Fall through */
		}
		
		DMClient oldClient = client;
		DMViewpoint oldViewpoint = viewpoint;
		boolean oldBypassCache = bypassCache;
		boolean oldBypassFilter = bypassFilter;

		try {
			client = null;
			viewpoint = model.getSystemViewpoint();
			bypassCache = true;
			
			// Using the SystemViewpoint as the viewpoint should disable almost all filtering,
			// but there are examples of filtering that might hide things from the system
			// viewpoint; trivially you can specify @DMFilter("false"). Also, we gain a 
			// little bit of efficiency by not doing filter computations.
			bypassFilter = true;
			
			T object = findUnchecked(storeKey);
			
			// There is some inefficiency here ... the property value is dehydrated twice;
			// once when the property is cached in the cache as a side effect of fetching
			// it, and again here. But dehydration is pretty fast, and doing it this
			// way is robust against the case where the property is *not* cached.
			// (because it's viewer specific, because the property is immediately
			// evicted from the cache again, etc.)
			try {
				return propertyHolder.dehydrate(propertyHolder.getRawPropertyValue(object));
			} catch (LazyInitializationException e2) {
				throw e2.getCause();
			}
			
		} finally {
			client = oldClient;
			viewpoint = oldViewpoint;
			bypassCache = oldBypassCache;
			bypassFilter = oldBypassFilter;
		}
	}

}
