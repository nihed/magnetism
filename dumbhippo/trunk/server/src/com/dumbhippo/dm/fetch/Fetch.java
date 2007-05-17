package com.dumbhippo.dm.fetch;

public class Fetch {
	private PropertyFetch[] properties;
	
	public Fetch(PropertyFetch[] properties) {
		this.properties = properties;
	}
	
	public PropertyFetch[] getProperties() {
		return properties;
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
