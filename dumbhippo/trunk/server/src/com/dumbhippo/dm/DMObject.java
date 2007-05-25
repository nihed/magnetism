package com.dumbhippo.dm;

import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

public abstract class DMObject<KeyType> {
	private KeyType key;
	private StoreKey storeKey;
	Guid guid;
	
	@SuppressWarnings("unchecked")
	protected DMObject(KeyType key) {
		this.storeKey = new StoreKey(getClassHolder(), key);
		this.key = key;
	}
	
	public KeyType getKey() {
		return key;
	}
	
	public StoreKey getStoreKey() {
		return storeKey;
	}
	
	protected abstract void init() throws NotFoundException;
	
	public abstract DMClassHolder getClassHolder();
	
	@SuppressWarnings("unchecked")
	static public <K,T extends DMObject<K>> Class<T> classCast(Class<K> keyClassclazz, Class<?> elementClass) {
		return (Class<T>)elementClass;
	}
}
