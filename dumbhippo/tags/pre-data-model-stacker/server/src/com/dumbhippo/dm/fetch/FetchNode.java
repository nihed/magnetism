package com.dumbhippo.dm.fetch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.schema.DMClassHolder;

public class FetchNode {
	static public final Logger logger = GlobalSetup.getLogger(FetchNode.class);
	
	private PropertyFetchNode[] properties;
	
	public FetchNode(PropertyFetchNode[] properties) {
		this.properties = properties;
		Arrays.sort(properties);
	}
	
	public PropertyFetchNode[] getProperties() {
		return properties;
	}
	
	public <K,T extends DMObject<K>> BoundFetch<K,T> bind(DMClassHolder<K,T> classHolder) {
		boolean includeDefault = false;
		
		List<PropertyFetch> boundProperties = new ArrayList<PropertyFetch>(properties.length);
		for (PropertyFetchNode property : properties) {
			if ("+".equals(property.getProperty()))
				includeDefault = true;
		}

		for (PropertyFetchNode property : properties) {
			if ("+".equals(property.getProperty()))
				continue;
			if ("*".equals(property.getProperty())) {
				// FIXME: We probably should make bind() throw an exception and make this fatal
				logger.warn("Ignoring '*' in property fetch string");
				continue;
			}
			property.bind(classHolder, includeDefault, boundProperties);
		}

		Collections.sort(boundProperties);
		
		return new BoundFetch<K,T>(boundProperties.toArray(new PropertyFetch[boundProperties.size()]), includeDefault, this);
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		for (int i = 0; i < properties.length; i++) {
			if (i != 0)
				b.append(';');
			b.append(properties[i].toString());
		}
		
		return b.toString();
	}

	public FetchNode merge(FetchNode other) {
		int count = 0;
		int i = 0;
		int j = 0;
		boolean changedProperties = false;
		
		while (true) {
			int cmp;
			
			if (i < properties.length && j < other.properties.length) {
				cmp = properties[i].compareTo(other.properties[j]);
			} else if (i < properties.length)
				cmp = -1;
			else if (j < other.properties.length)
				cmp = 1;
			else
				break;
			
			if (cmp == 0) {
				if (!properties[i].equals(other.properties[j]))
					changedProperties = true; 

				i++;
				j++;
			} else if (cmp < 0) {
				i++;
			} else {
				j++;
			}
				
			count++;
		}
		
		// If the other property is a subset of this one, we can just return this one
		// and vice-versa
		if (!changedProperties) {
			if (count == properties.length)
				return this;
			else if (count == other.properties.length)
				return other;
		}
		
		PropertyFetchNode[] newProperties = new PropertyFetchNode[count];

		count = 0; i = 0; j = 0;
		
		while (true) {
			int cmp;
			
			if (i < properties.length && j < other.properties.length)
				cmp = properties[i].compareTo(other.properties[j]);
			else if (i < properties.length)
				cmp = -1;
			else if (j < other.properties.length)
				cmp = 1;
			else
				break;
			
			logger.debug("{} {} {}", new Object[] { 
					i < properties.length ? properties[i].getProperty() : null, 
					j < other.properties.length ? other.properties[j].getProperty() : null, 
					cmp });
			
			if (cmp == 0) {
				newProperties[count] = properties[i].merge(other.properties[j]);
				i++;
				j++;
			} else if (cmp < 0) {
				newProperties[count] = properties[i];
				i++;
			} else {
				newProperties[count] = other.properties[j];
				j++;
			}
				
			count++;
		}
		
		return new FetchNode(newProperties);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FetchNode))
			return false;
		
		FetchNode other = (FetchNode)o;
		if (other.properties.length != properties.length)
			return false;
		
		for (int i = 0; i < properties.length; i++)
			if (!other.properties[i].equals(properties[i]))
				return false;
		
		return true;
	}
}
