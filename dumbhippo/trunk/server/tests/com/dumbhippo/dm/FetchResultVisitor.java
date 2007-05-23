package com.dumbhippo.dm;

import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;

public class FetchResultVisitor implements FetchVisitor {
	FetchResultResource currentResource;
	FetchResult result = new FetchResult(null);
	
	private String makeResourceId(DMClassHolder classHolder, Object key) {
		String resourceBase = classHolder.getResourceBase();
		return "http://mugshot.org" + resourceBase + "/" + key.toString();
	}

	public <T extends DMObject> void beginResource(DMClassHolder<T> classHolder, Object key, Fetch<T> fetch, boolean indirect) {
		String resourceId = makeResourceId(classHolder, key);
		currentResource = new FetchResultResource(classHolder.getClassId(), resourceId, fetch.makeFetchString(classHolder), indirect);
	}

	public void endResource() {
		result.addResource(currentResource);
		currentResource = null;
	}

	public void plainProperty(DMPropertyHolder propertyHolder, Object value) {
		FetchResultProperty property = FetchResultProperty.createSimple(propertyHolder.getName(), propertyHolder.getNameSpace());
		property.setValue(value.toString());
		currentResource.addProperty(property);
	}

	public void resourceProperty(DMPropertyHolder propertyHolder, Object key) {
		DMClassHolder<? extends DMObject> classHolder = propertyHolder.getResourceClassHolder();
		String resourceId = makeResourceId(classHolder, key);
		
		FetchResultProperty property = FetchResultProperty.createResource(propertyHolder.getName(), propertyHolder.getNameSpace(), resourceId);
		currentResource.addProperty(property);
	}
	
	public FetchResult getResult() {
		return result;
	}
}
