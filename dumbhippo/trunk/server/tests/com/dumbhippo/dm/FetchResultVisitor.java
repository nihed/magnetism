package com.dumbhippo.dm;

import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.PlainPropertyHolder;
import com.dumbhippo.dm.schema.ResourcePropertyHolder;
import com.dumbhippo.dm.store.StoreClient;

public class FetchResultVisitor implements FetchVisitor {
	FetchResultResource currentResource;
	FetchResult result = new FetchResult(null);
	private TestDMClient client;
	
	public FetchResultVisitor(TestDMClient client) {
		this.client = client;
	}
	
	public StoreClient getClient() {
		return client != null ? client.getStoreClient() : null;
	}

	public boolean getNeedFetch() {
		return true;
	}

	private String makeResourceId(DMClassHolder classHolder, Object key) {
		String resourceBase = classHolder.getResourceBase();
		return "http://mugshot.org" + resourceBase + "/" + key.toString();
	}

	public <K,T extends DMObject<K>> void beginResource(DMClassHolder<T> classHolder, K key, String fetchString, boolean indirect) {
		String resourceId = makeResourceId(classHolder, key);
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

	public <KP,TP extends DMObject<KP>> void resourceProperty(ResourcePropertyHolder<KP,TP> propertyHolder, KP key) {
		DMClassHolder<TP> classHolder = propertyHolder.getResourceClassHolder();
		String resourceId = makeResourceId(classHolder, key);
		
		FetchResultProperty property = FetchResultProperty.createResource(propertyHolder.getName(), propertyHolder.getNameSpace(), resourceId);
		currentResource.addProperty(property);
	}
	
	public FetchResult getResult() {
		return result;
	}
}
