package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	private boolean compareFetchStrings(String fetchA, String fetchB) {
		if (fetchA == null && fetchB == null)
			return true;
		if (fetchA == null || fetchB == null)
			return false;
		
		// Short-circuit for the common case of "*", "*", etc
		if (fetchA.equals(fetchB))
			return true;
		
		String[] componentsA = fetchA.split(";");
		String[] componentsB = fetchB.split(";");
		
		if (componentsA.length != componentsB.length)
			return false;
		
		Arrays.sort(componentsA);
		Arrays.sort(componentsB);
		
		for (int i = 0; i < componentsA.length; i++)
			if (!componentsA[i].equals(componentsB[i]))
				return false;
		
		return true;
	}

	public void validateAgainst(FetchResultResource other) throws FetchValidationException {
		if (!resourceId.equals(other.resourceId))
			throw new FetchValidationException("Expected resourceId %s, got %s", other.resourceId, resourceId);

		if (!classId.equals(other.classId))
			throw new FetchValidationException("%s: expected classId %s, got %s", resourceId, other.classId, classId);
		
		if (!compareFetchStrings(fetch, other.fetch))
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
	
	static final Pattern PARAMETER = Pattern.compile("\\$\\(([A-Za-z_][A-Za-z0-9_]+)\\)");

	public static String substituteResourceId(String resourceId, Map<String, String> parametersMap) {
		StringBuffer sb = new StringBuffer();
		Matcher m = PARAMETER.matcher(resourceId);
		while (m.find()) {
			String replacement = parametersMap.get(m.group(1));
			if (replacement == null)
				throw new RuntimeException("No replacement for parameter '" + m.group(1) + "'");
		    m.appendReplacement(sb, replacement);
		}
		 m.appendTail(sb);
		 return sb.toString();
	}

	public FetchResultResource substitute(Map<String, String> parametersMap) {
		String newResourceId = substituteResourceId(resourceId, parametersMap);
		FetchResultResource substituted = new FetchResultResource(classId, newResourceId, fetch);
		
		for (FetchResultProperty property : properties)
			substituted.addProperty(property.substitute(parametersMap));
		
		return substituted;
	}

	public int compareTo(FetchResultResource other) {
		return resourceId.compareTo(other.resourceId);
	}
}
