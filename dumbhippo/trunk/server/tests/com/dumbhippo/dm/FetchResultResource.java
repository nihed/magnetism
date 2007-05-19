package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.List;

public class FetchResultResource {
	private List<FetchResultProperty> properties = new ArrayList<FetchResultProperty>();
	private String resourceId;
	private String classId;
	private String fetch;

	public FetchResultResource(String classId, String resourceId, String fetch) {
		this.classId = classId;
		this.resourceId = resourceId;
		this.fetch = fetch;
	}
	
	public void addProperty(FetchResultProperty property) {
		this.properties.add(property);
	}

	public List<FetchResultProperty> getProperties() {
		return properties;
	}

	public String getClassId() {
		return classId;
	}

	public String getResourceId() {
		return resourceId;
	}

	public String getFetch() {
		return fetch;
	}
}
