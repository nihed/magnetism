package com.dumbhippo.dm.fetch;

import com.dumbhippo.dm.DMPropertyHolder;


public class PropertyFetch {
	private DMPropertyHolder property;
	private Fetch children;
	private FetchAttribute[] attributes;
	
	public PropertyFetch(DMPropertyHolder property, FetchAttribute[] attributes, Fetch children) {
		this.property = property;
		this.attributes = attributes;
		this.children = children;
	}

	public Fetch getChildren() {
		return children;
	}

	public DMPropertyHolder getProperty() {
		return property;
	}
	
	public Object getAttribute(FetchAttributeType type) {
		for (FetchAttribute attr : attributes) {
			if (attr.getType() == type)
				return attr.getValue();
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		b.append(property.getPropertyId());
		
		if (attributes.length > 0) {
			b.append('(');
			for (int i = 0; i < attributes.length; i++) {
				if (i != 0)
					b.append(',');
				b.append(attributes[i].toString());
			}
			b.append(')');
		}
		
		if (children != null) {
			b.append(' ');
			if (children.getProperties().length == 1) {
				b.append(children.getProperties()[0].toString());
			} else {
				b.append('[');
				b.append(children.toString());
				b.append(']');
			}
		}
		
		return b.toString();
	}
	
}
