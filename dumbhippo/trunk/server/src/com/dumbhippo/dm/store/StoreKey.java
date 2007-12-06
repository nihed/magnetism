package com.dumbhippo.dm.store;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.schema.DMClassHolder;

public class StoreKey<K,T extends DMObject<K>> {
	protected DMClassHolder<K,T> classHolder;
	protected K key;

	public StoreKey(DMClassHolder<K,T> classHolder, K key) {
		this.classHolder = classHolder;
		this.key = key;
	}
	
	public void setKey(K key) {
		this.key = key;
	}
	
	public K getKey() {
		return key;
	}
	
	public DMClassHolder<K,T> getClassHolder() {
		return classHolder;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof StoreKey))
			return false;
		
		StoreKey<?,?> other = (StoreKey<?,?>)o;
		
		return classHolder == other.classHolder &&
			key.equals(other.key);
	}
	
	@Override
	public int hashCode() {
		return 11 * classHolder.hashCode() + 17 * key.hashCode();
	}
	
	@Override
	public String toString() {
		return classHolder.getDMOClass().getSimpleName() + "#" + key.toString();
	}
}
