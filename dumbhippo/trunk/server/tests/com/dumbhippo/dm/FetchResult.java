package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dumbhippo.XmlBuilder;

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
	
	public List<FetchResultResource> getResources() {
		return resources;
	}
	
	public void writeToXmlBuilder(XmlBuilder builder) {
		builder.openElement("fetchResult", 
				"id", id,
				"xmlns:m", FetchResultHandler.MUGSHOT_SYSTEM_NS);
		
		for (FetchResultResource resource : resources) {
			resource.writeToXmlBuilder(builder);
		}
		
		builder.closeElement();
	}
	
	public void validateAgainst(FetchResult other) throws FetchValidationException {
		List<FetchResultResource> sortedResources = new ArrayList<FetchResultResource>(resources);
		Collections.sort(sortedResources);
		
		List<FetchResultResource> otherResources = new ArrayList<FetchResultResource>(other.resources);
		Collections.sort(otherResources);
		
		int count = Math.max(sortedResources.size(), otherResources.size());
		for (int i = 0; i < count; i++) {
			FetchResultResource resource = null; 
			FetchResultResource otherResource = null;
			try { resource = sortedResources.get(i); } catch (IndexOutOfBoundsException e) {}
			try { otherResource = otherResources.get(i); } catch (IndexOutOfBoundsException e) {}
			
			int comparison = 0;
			if (resource != null && otherResource != null)
				comparison = resource.compareTo(otherResource);
			
			if (otherResource == null || comparison < 0) {
				throw new FetchValidationException("Found unexpected resource %s", resource.getResourceId());
			} else if (resource == null || comparison > 0) {
				throw new FetchValidationException("Didn't find expected resource %s", otherResource.getResourceId());
			}
			
			resource.validateAgainst(otherResource);
		}
	}
}
