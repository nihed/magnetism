package com.dumbhippo.dm.fetch;

import java.util.List;

import com.dumbhippo.dm.DMClassHolder;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMPropertyHolder;


public class PropertyFetchNode {
	private String property;
	private FetchNode children;
	private FetchAttribute[] attributes;
	
	public PropertyFetchNode(String property, FetchAttribute[] attributes, FetchNode children) {
		this.property = property;
		this.attributes = attributes;
		this.children = children;
	}

	public FetchNode getChildren() {
		return children;
	}

	public String getProperty() {
		return property;
	}
	
	public Object getAttribute(FetchAttributeType type) {
		for (FetchAttribute attr : attributes) {
			if (attr.getType() == type)
				return attr.getValue();
		}
		
		return null;
	}
	

	/**
	 * Finds all properties in the given class and in *subclasses* of the given class
	 * that match this node, bind them and return the result.
	 * 
	 * TODO: Implement the subclass part
	 * 
	 * @param classHolder
	 * @param whether to skip properties that are marked as defaultInclude
	 * @param resultList list to append the results to 
	 */
	public void bind(DMClassHolder<? extends DMObject> classHolder, boolean skipDefault, List<PropertyFetch> resultList) {
		int propertyIndex = classHolder.getPropertyIndex(property);
		if (propertyIndex >= 0) {
			DMPropertyHolder propertyHolder = classHolder.getProperty(propertyIndex);
			if (propertyHolder.getDefaultInclude() && skipDefault)
				return;
			
			Fetch boundChildren = null;
			if (children != null && propertyHolder.isResourceValued()) {
				DMClassHolder<? extends DMObject> childClassHolder = classHolder.getModel().getDMClass(propertyHolder.getResourceType());
				boundChildren = children.bind(childClassHolder);
			}
			
			resultList.add(new PropertyFetch(propertyHolder, attributes, boundChildren));
		}
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		b.append(property);
		
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
