package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.dumbhippo.XmlBuilder;

public class FetchResultResource implements Comparable<FetchResultResource> {
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
	
	public void writeToXmlBuilder(XmlBuilder builder) {
		builder.openElement("resource", 
				"xmlns", classId,
				"m:resourceId", resourceId,
				"m:fetch", fetch);
		
		for (FetchResultProperty property : properties)
			property.writeToXmlBuilder(builder, classId);
		
		builder.closeElement();
	}

	public void validateAgainst(FetchResultResource other) throws FetchValidationException {
		if (!resourceId.equals(other.resourceId))
			throw new FetchValidationException("Expected resourceId %s, got %s", other.resourceId, resourceId);

		if (!classId.equals(other.classId))
			throw new FetchValidationException("%s: expected classId %s, got %s", resourceId, other.classId, classId);
		
		if (!((fetch == null && other.fetch == null) || fetch.equals(other.fetch)))
			throw new FetchValidationException("%s: expected fetch %s, got %s", resourceId, other.fetch, fetch);
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		List<FetchResultProperty> sortedProperties = new ArrayList<FetchResultProperty>(properties);
		Collections.sort(sortedProperties);
		
		List<FetchResultProperty> otherProperties = new ArrayList<FetchResultProperty>(other.properties);
		Collections.sort(otherProperties);
		
		int count = Math.max(sortedProperties.size(), otherProperties.size());
		for (int i = 0; i < count; i++) {
			FetchResultProperty property = null; 
			FetchResultProperty otherProperty = null;
			try { property = sortedProperties.get(i); } catch (IndexOutOfBoundsException e) {}
			try { otherProperty = otherProperties.get(i); } catch (IndexOutOfBoundsException e) {}
			
			int comparison = 0;
			if (property != null && otherProperty != null)
				comparison = property.compareTo(otherProperty);
			
			if (otherProperty == null || comparison < 0) {
				throw new FetchValidationException("%s: Found unexpected property %s, value=%s", resourceId, property.getPropertyId(), property.getValue());
			} else if (property == null || comparison > 0) {
				throw new FetchValidationException("%s: Didn't find expected property %s, value=%s", resourceId, otherProperty.getPropertyId(), otherProperty.getValue());
			}
			
			property.validateAgainst(resourceId, otherProperty);
		}
	}

	public int compareTo(FetchResultResource other) {
		return resourceId.compareTo(other.resourceId);
	}
}
