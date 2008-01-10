package com.dumbhippo.dm.fetch;

import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.schema.ResourcePropertyHolder;


public class PropertyFetchNode implements Comparable<PropertyFetchNode> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PropertyFetchNode.class); 
	
	private String property;
	private FetchNode children;
	private FetchAttributeNode[] attributes;
	
	public PropertyFetchNode(String property, FetchAttributeNode[] attributes, FetchNode children) {
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
		for (FetchAttributeNode attr : attributes) {
			if (attr.getType() == type)
				return attr.getValue();
		}
		
		return null;
	}

	public <K,T extends DMObject<K>> void bindResourceProperty(ResourcePropertyHolder<?,?,K,T> resourceHolder, List<PropertyFetch> resultList, boolean maybeSkip, boolean notify, int max) {
		BoundFetch<K,T> defaultChildren = resourceHolder.getDefaultChildren();
		BoundFetch<K,T> boundChildren = null;
		
		if (maybeSkip && children == null && defaultChildren == null) {
			return;
		}

		if (children != null) {
			DMClassHolder<K,T> childClassHolder = resourceHolder.getResourceClassHolder();
			boundChildren = children.bind(childClassHolder);
		}

		if (maybeSkip && boundChildren != null && defaultChildren != null) {
			if (boundChildren.equals(defaultChildren))
				return;
		}
		
		resultList.add(new PropertyFetch(resourceHolder, boundChildren, notify, max));
	}

	public void bindPlainProperty(DMPropertyHolder<?,?,?> propertyHolder, List<PropertyFetch> resultList, boolean maybeSkip, boolean notify, int max) {
		if (!maybeSkip)
			resultList.add(new PropertyFetch(propertyHolder, null, notify, max));
	}
	
	public void bindToClass(DMClassHolder<?,?> classHolder, boolean skipDefault, List<PropertyFetch> resultList, int max, boolean notify) {
		int propertyIndex = classHolder.getPropertyIndex(property);
		if (propertyIndex >= 0) {
			DMPropertyHolder<?,?,?> propertyHolder = classHolder.getProperty(propertyIndex);
			boolean maybeSkip = skipDefault && propertyHolder.getDefaultInclude(); 
			
			if (propertyHolder instanceof ResourcePropertyHolder) {
				bindResourceProperty(propertyHolder.asResourcePropertyHolder(propertyHolder.getKeyClass()), resultList, maybeSkip, notify, max);
			} else {
				bindPlainProperty(propertyHolder, resultList, maybeSkip, notify, max);
			}
		}
	}

	// FIXME: We should probably validate the types during or immediately after parsing

	private boolean getNotify() {
		for (FetchAttributeNode attr : attributes) {
			if (attr.getType() == FetchAttributeType.NOTIFY &&
				attr.getValue() instanceof Boolean)
				return (Boolean)attr.getValue();
		}
		
		return true;
	}
	
	private int getMax() {
		for (FetchAttributeNode attr : attributes) {
			if (attr.getType() == FetchAttributeType.MAX &&
				attr.getValue() instanceof Integer)
				return (Integer)attr.getValue();
		}
		
		return -1;
	}

	/**
	 * Finds all properties in the given class and in *subclasses* of the given class
	 * that match this node, bind them and return the result.
	 * 
	 * @param classHolder
	 * @param whether to skip properties that are marked as defaultInclude 
	 *   (the property will still not be skipped if the children overridden)
	 * @param resultList list to append the results to 
	 */
	public void bind(DMClassHolder<?,?> classHolder, boolean skipDefault, List<PropertyFetch> resultList) {
		int max = getMax();
		boolean notify = getNotify();
		
		bindToClass(classHolder, skipDefault, resultList, max, notify);
		for (DMClassHolder<?,?> subclassHolder : classHolder.getDerivedClasses())
			bindToClass(subclassHolder, skipDefault, resultList, max, notify);
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

	/* This sort is used in FetchNode to sort properties to make merging easy */
	public int compareTo(PropertyFetchNode other) {
		return property.compareTo(other.property);
	}

	public PropertyFetchNode merge(PropertyFetchNode other) {
		assert(property.equals(other.property));
		
		FetchNode newChildren;
		
		if (other.children == null)
			newChildren = children;
		else if (children == null)
			newChildren = other.children;
		else
			newChildren = children.merge(other.children);

		FetchAttributeNode newAttr[];

		if (other.attributes.length == 0)
			newAttr = attributes;
		else if (attributes.length == 0)
			newAttr = other.attributes;
		else {
			int newMax = Math.max(getMax(), other.getMax());
			boolean newNotify = getNotify() || other.getNotify();
			
			if (newMax >= 0 && !newNotify)
				newAttr = new FetchAttributeNode[] {
						new FetchAttributeNode(FetchAttributeType.MAX, newMax),
						new FetchAttributeNode(FetchAttributeType.NOTIFY, newNotify),
			    };
			else if (newMax >= 0)
				newAttr = new FetchAttributeNode[] {
						new FetchAttributeNode(FetchAttributeType.MAX, newMax)
			    };
			else if (!newNotify)
				newAttr = new FetchAttributeNode[] {
						new FetchAttributeNode(FetchAttributeType.NOTIFY, newNotify),
			    };
			else
				newAttr = new FetchAttributeNode[0];
		}
		
		// Common special case is "name".merge("name"), avoid allocation for that
		if (newAttr == attributes && newChildren == children)
			return this;
		else if (newAttr == other.attributes && newChildren == other.children)
			return other;
		else
			return new PropertyFetchNode(property, newAttr, newChildren);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PropertyFetchNode))
			return false;
		
		PropertyFetchNode other = (PropertyFetchNode)o;
		
		if (!other.property.equals(property))
			return false;
		
		// We count "property []" as different than "property" for convenience 
		if (children != null) {
			if (!children.equals(other.children))
				return false;
		} else {
			if (other.children != null)
				return false;
		}
		
		if (getMax() != other.getMax())
			return false;
		
		if (getNotify() != other.getNotify())
			return false;
		
		return true;
	}
}
