package com.dumbhippo.dm;

public class FetchResultProperty {
	private String name;
	private String url;
	private String value;
	private boolean resource;

	static public FetchResultProperty createSimple(String name, String url) {
		return new FetchResultProperty(name, url, null, false);
	}
	
	static public FetchResultProperty createResource(String name, String url, String resourceId) {
		return new FetchResultProperty(name, url, resourceId, true);
	}
	
	private FetchResultProperty(String name, String url, String value, boolean resource) {
		this.name = name;
		this.url = url;
		this.value = value;
		this.resource = resource;
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public Object getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public boolean isResource() {
		return resource;
	}
}
