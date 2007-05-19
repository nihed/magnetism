package com.dumbhippo.dm.fetch;

public class Fetch {
	private PropertyFetch[] properties;
	boolean includeDefault;
	
	public Fetch(PropertyFetch[] properties, boolean includeDefault) {
		this.properties = properties;
		this.includeDefault = includeDefault;
	}
	
	public PropertyFetch[] getProperties() {
		return properties;
	}
	
	public boolean getIncludeDefault() {
		return includeDefault;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		if (includeDefault)
			b.append('+');
		
		for (int i = 0; i < properties.length; i++) {
			if (i != 0 || includeDefault)
				b.append(';');
			b.append(properties[i].toString());
		}
		
		return b.toString();
	}
}
