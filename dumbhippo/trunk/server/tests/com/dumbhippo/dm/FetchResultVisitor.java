package com.dumbhippo.dm;

import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.schema.PlainPropertyHolder;
import com.dumbhippo.dm.schema.ResourcePropertyHolder;

public class FetchResultVisitor implements FetchVisitor {
	FetchResultResource currentResource;
	FetchResult result = new FetchResult(null);

	public FetchResultVisitor() {
	}
	
	public boolean getNeedFetch() {
		return true;
	}

	public <K,T extends DMObject<K>> void beginResource(DMClassHolder<K,T> classHolder, K key, String fetchString, boolean indirect) {
		String resourceId = classHolder.makeResourceId(key);
		currentResource = new FetchResultResource(classHolder.getClassId(), resourceId, fetchString, indirect);
	}

	public void endResource() {
		result.addResource(currentResource);
		currentResource = null;
	}

	public void plainProperty(PlainPropertyHolder propertyHolder, Object value) {
		FetchResultProperty property = FetchResultProperty.createSimple(propertyHolder.getName(), propertyHolder.getNameSpace());
		property.setValue(value.toString());
		currentResource.addProperty(property);
	}

	public <KP,TP extends DMObject<KP>> void resourceProperty(ResourcePropertyHolder<?,?,KP,TP> propertyHolder, KP key) {
		DMClassHolder<KP,TP> classHolder = propertyHolder.getResourceClassHolder();
		String resourceId = classHolder.makeResourceId(key);
		
		FetchResultProperty property = FetchResultProperty.createResource(propertyHolder.getName(), propertyHolder.getNameSpace(), resourceId);
		currentResource.addProperty(property);
	}
	
	public <KP, TP extends DMObject<KP>> void feedProperty(ResourcePropertyHolder<?, ?, KP, TP> propertyHolder, KP key, long timestamp, boolean incremental) {
		DMClassHolder<KP,TP> classHolder = propertyHolder.getResourceClassHolder();
		String resourceId = classHolder.makeResourceId(key);
		
		FetchResultProperty property = FetchResultProperty.createFeed(propertyHolder.getName(), propertyHolder.getNameSpace(), resourceId, timestamp);
		currentResource.addProperty(property);
	}
	
	public FetchResult getResult() {
		return result;
	}

	public void emptyProperty(DMPropertyHolder propertyHolder) {
		// FIXME: test this
	}
}
