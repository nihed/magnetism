package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.List;

public class FetchResult {
	private String id;
	private List<FetchResultResource> resources = new ArrayList<FetchResultResource>();

	public FetchResult(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public void addResource(FetchResultResource resource) {
		resources.add(resource);
	}
	
	public List<FetchResultResource> getResource() {
		return resources;
	}
}
