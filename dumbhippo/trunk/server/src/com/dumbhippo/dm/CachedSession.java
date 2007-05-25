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
	private DMViewpoint viewpoint;

	private Map<StoreKey, DMObject> sessionDMOs = new HashMap<StoreKey, DMObject>();

	protected CachedSession(DataModel model, DMViewpoint viewpoint) {
		super(model);
		this.viewpoint = viewpoint;
		viewpoint.setSession(this);
	}

	@Override
	public DMViewpoint getViewpoint() {
		return viewpoint;
	}
	
	private <K, T extends DMObject<K>> T findRawObject(StoreKey<K,T> storeKey) {
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
		
		// Despite the fact that we aren't supposed to return filtered results,
		// we have to filter before storing into the local cache (or alternatively, we'd
		// have to filter before returning a result from the local cache)
		
		T filtered = filterRawObject(storeKey, raw);
		if (filtered != null)
			sessionDMOs.put(storeKey, filtered);
		
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

		sessionDMOs.put(storeKey, filtered);
		
		return filtered;
	}
}
