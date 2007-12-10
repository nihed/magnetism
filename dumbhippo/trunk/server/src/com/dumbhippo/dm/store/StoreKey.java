package com.dumbhippo.dm.store;

import com.dumbhippo.dm.DMKey;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.filter.CompiledFilter;
import com.dumbhippo.dm.schema.DMClassHolder;

public class StoreKey<K,T extends DMObject<K>> implements Cloneable {
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
	
	public boolean isVisible(DMViewpoint viewpoint) {
		CompiledFilter<K,T> filter = classHolder.getFilter();
		if (filter != null)
			return filter.filterKey(viewpoint, key) != null;
		else
			return true;
	}
	
	@Override
	public StoreKey<K,T> clone() {
		if (key instanceof DMKey) {
			@SuppressWarnings("unchecked")
			K clonedKey = (K)((DMKey)key).clone();
			if (clonedKey == key)
				return this;
			else
				return new StoreKey<K,T>(classHolder, clonedKey);
		} else {
			return this;
		}
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
