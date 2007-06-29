package com.dumbhippo.dm;

import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.server.NotFoundException;

/*
 * UnfilteredSession is a session we use when we need to get a property value without
 * invoking the filtering mechanisms. The typical time we need to do this is when
 * evaluating a condition in a filter.
 */
public class UnfilteredSession extends DMSession {
	protected UnfilteredSession(DataModel model) {
		super(model);
	}

	@Override
	public DMClient getClient() {
		return null;
	}
	
	@Override
	public DMViewpoint getViewpoint() {
		return model.getSystemViewpoint();
	}

	@Override
	public <K, T extends DMObject<K>> T findUnchecked(StoreKey<K, T> storeKey) {
		return storeKey.getClassHolder().createInstance(storeKey.getKey(), this);
	}


	@Override
	public <K, T extends DMObject<K>> T find(StoreKey<K, T> storeKey) throws NotFoundException {
		return findUnchecked(storeKey);
	}

	@Override
	public <K, T extends DMObject<K>> Object fetchAndFilter(StoreKey<K, T> key, int propertyIndex) throws NotCachedException {
		throw new NotCachedException();
	}

	@Override
	public <K, T extends DMObject<K>> Object storeAndFilter(StoreKey<K, T> key, int propertyIndex, Object value) {
		return value;
	}

	@Override
	public void afterCompletion(int status) {
	}
}
