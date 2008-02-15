package com.dumbhippo.dm.fetch;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.schema.PlainPropertyHolder;
import com.dumbhippo.dm.schema.ResourcePropertyHolder;

public interface FetchVisitor {
	boolean getNeedFetch();
	
	<K,T extends DMObject<K>> void beginResource(DMClassHolder<K,T> classHolder, K key, String fetchString, boolean indirect);
	<K,T extends DMObject<K>> void plainProperty(PlainPropertyHolder<K, T, ?> propertyHolder, Object value);
	<KP,TP extends DMObject<KP>> void resourceProperty(ResourcePropertyHolder<?,?,KP,TP> propertyHolder, KP key);
	<KP,TP extends DMObject<KP>> void feedProperty(ResourcePropertyHolder<?,?,KP,TP> propertyHolder, KP key, long timestamp, boolean incremental);
	<K,T extends DMObject<K>> void emptyProperty(DMPropertyHolder<K,T,?> propertyHolder);
	void endResource();
	
	// These are used only for notifications
	<K,T extends DMObject<K>> void evictedResource(DMClassHolder<K,T> classHolder, K key);	
	<K,T extends DMObject<K>> void deletedResource(DMClassHolder<K,T> classHolder, K key);	
}
