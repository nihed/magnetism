package com.dumbhippo.dm.store;

import com.dumbhippo.dm.NotCachedException;

public class StoreNode {
	private static Object nil = new Boolean(false);

	private long timestamp = -1;
	private Object[] properties;

	StoreNode(int propertyCount) {
		this.properties = new Object[propertyCount];
	}
	
	public synchronized Object fetch(int propertyIndex) throws NotCachedException {
		Object value = properties[propertyIndex];
		if (value == null)
			throw new NotCachedException();
		
		return value != nil ? value : null;
	}
	
	public synchronized void store(int propertyIndex, Object value, long timestamp) {
		if (this.timestamp > timestamp)
			return;
		
		properties[propertyIndex] = value != null ? value : nil;
	}
	
	public synchronized void invalidate(int propertyIndex, long timestamp) {
		this.timestamp = timestamp;
		properties[propertyIndex] = null;
	}
}
