package com.dumbhippo.dm.fetch;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMClassHolder;
import com.dumbhippo.dm.DMObject;

public class FetchNode {
	static public final Logger logger = GlobalSetup.getLogger(FetchNode.class);
	
	private PropertyFetchNode[] properties;
	
	public FetchNode(PropertyFetchNode[] properties) {
		this.properties = properties;
	}
	
	public PropertyFetchNode[] getProperties() {
		return properties;
	}
	
	public Fetch bind(DMClassHolder<? extends DMObject> classHolder) {
		boolean includeDefault = false;
		
		List<PropertyFetch> boundProperties = new ArrayList<PropertyFetch>(properties.length);
		for (PropertyFetchNode property : properties) {
			if ("+".equals(property.getProperty()))
				includeDefault = true;
		}

		for (PropertyFetchNode property : properties) {
			if ("+".equals(property.getProperty()))
				continue;
			if ("*".equals(property.getProperty()))
				continue; // * is ignored in a fetch-string specification
		
			property.bind(classHolder, includeDefault, boundProperties);
		}
		
		return new Fetch(boundProperties.toArray(new PropertyFetch[boundProperties.size()]), includeDefault);
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
}
