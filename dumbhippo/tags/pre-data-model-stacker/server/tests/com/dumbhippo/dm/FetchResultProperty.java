package com.dumbhippo.dm;

import java.util.Map;

import com.dumbhippo.XmlBuilder;

public class FetchResultProperty implements Comparable<FetchResultProperty> {
	private String name;
	private String namespace;
	private String propertyId;
	private String value;
	private boolean resource;
	private long timestamp;

	static public FetchResultProperty createSimple(String name, String namespace) {
		return new FetchResultProperty(name, namespace, null, false, 1);
	}
	
	static public FetchResultProperty createResource(String name, String namespace, String resourceId) {
		return new FetchResultProperty(name, namespace, resourceId, true, -1);
	}
	
	public static FetchResultProperty createFeed(String name, String namespace, String resourceId, long timestamp) {
		return new FetchResultProperty(name, namespace, resourceId, true, timestamp);
	}
	
	private FetchResultProperty(String name, String namespace, String value, boolean resource, long timestamp) {
		this.name = name;
		this.namespace = namespace;
		this.propertyId = namespace + "#" + name;
		this.value = value;
		this.resource = resource;
		this.timestamp = timestamp;
	}

	public String getName() {
		return name;
	}

	public String getPropertyId() {
		return propertyId;
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
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public void writeToXmlBuilder(XmlBuilder builder, String defaultNS) {
		String ns = defaultNS.equals(namespace) ? null : namespace;
		
		if (resource && timestamp >= 0)
			builder.appendEmptyNode(name, "m:resourceId", value, "m:ts", Long.toString(timestamp), "xmlns", ns);
		else if (resource)
			builder.appendEmptyNode(name, "m:resourceId", value, "xmlns", ns);
		else
			builder.appendTextNode(name, value, "xmlns", ns);
	}
	
	public void validateAgainst(String context, FetchResultProperty other) throws FetchValidationException {
		if (!getPropertyId().equals(other.getPropertyId()))
			throw new FetchValidationException("%s: Expected propertyId %s, got %s", other.propertyId, propertyId);

		if (!value.equals(other.value))
			throw new FetchValidationException("%s:%s expected value %s, got %s", context, propertyId,  other.value, value);

		if (resource != other.resource)
			throw new FetchValidationException("%s:%s expected isResource %s, got %s", context, propertyId,  other.resource, resource);
		
		if (timestamp != other.timestamp)
			throw new FetchValidationException("%s:%s expected timestamp %ld, got %ld", context, propertyId, other.timestamp, timestamp);
	}

	public FetchResultProperty substitute(Map<String, String> parametersMap) {
		if (resource) {
			String newResourceId = FetchResultResource.substituteResourceId(value, parametersMap);
			if (timestamp >= 0)
				return createFeed(name, namespace, newResourceId, timestamp);
			else
				return createResource(name, namespace, newResourceId);
		} else {
			return this;
		}
	}

	public int compareTo(FetchResultProperty other) {
		int v = propertyId.compareTo(other.propertyId);
		if (v != 0)
			return v;
		
		return value.compareTo(other.value);
	}
}
