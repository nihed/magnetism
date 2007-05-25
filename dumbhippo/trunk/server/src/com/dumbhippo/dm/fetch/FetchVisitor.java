package com.dumbhippo.dm.fetch;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.PlainPropertyHolder;
import com.dumbhippo.dm.schema.ResourcePropertyHolder;
import com.dumbhippo.dm.store.StoreClient;

public interface FetchVisitor {
	StoreClient getClient();
	boolean getNeedFetch();
	
	<K,T extends DMObject<K>> void beginResource(DMClassHolder<T> classHolder, K key, String fetchString, boolean indirect);
	void plainProperty(PlainPropertyHolder propertyHolder, Object value);
	<KP,TP extends DMObject<KP>> void resourceProperty(ResourcePropertyHolder<KP,TP> propertyHolder, KP key);
	void endResource();
}
