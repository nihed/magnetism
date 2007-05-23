package com.dumbhippo.dm.fetch;

import com.dumbhippo.dm.DMClassHolder;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMPropertyHolder;

public interface FetchVisitor {
	<T extends DMObject> void beginResource(DMClassHolder<T> classHolder, Object key, Fetch<T> fetch, boolean indirect);
	void plainProperty(DMPropertyHolder propertyHolder, Object value);
	void resourceProperty(DMPropertyHolder propertyHolder, Object key);
	void endResource();
}
