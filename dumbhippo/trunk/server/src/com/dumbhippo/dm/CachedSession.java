package com.dumbhippo.dm;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.filter.CompiledFilter;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.server.NotFoundException;

public abstract class CachedSession extends DMSession {
	private static Logger logger = GlobalSetup.getLogger(CachedSession.class);
	private DMClient client;
	private DMViewpoint viewpoint;

	private Map<StoreKey, DMObject> sessionDMOs = new HashMap<StoreKey, DMObject>();
	
	// Set true when we are running with a temporary viewpoint ("su"); in that case
	// we must bypass the cache to avoid cross-polution with the parent viewpoint
	private boolean bypassCache;

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
		logger.debug("Didn't find object for key {}, creating a new one", storeKey.getKey());
		return storeKey.getClassHolder().createInstance(storeKey.getKey(), this);
	}
	

	private <K, T extends DMObject<K>> T filterRawObject(StoreKey<K,T> storeKey, T raw) {
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

	public void runWithViewpoint(DMViewpoint newViewpoint, Runnable runnable) {
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
	
	public void runAsSystem(Runnable runnable) {
		runWithViewpoint(model.getSystemViewpoint(), runnable);
	}
}
