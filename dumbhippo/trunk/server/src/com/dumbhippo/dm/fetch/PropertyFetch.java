package com.dumbhippo.dm.fetch;

import com.dumbhippo.dm.schema.DMPropertyHolder;


public final class PropertyFetch implements Comparable<PropertyFetch> {
	private DMPropertyHolder<?,?,?> property;
	private Fetch<?,?> children;
	private boolean notify;
	private int max;
	
	public PropertyFetch(DMPropertyHolder<?,?,?> property, Fetch<?,?> children, boolean notify, int max) {
		this.property = property;
		this.children = children;
		this.notify = notify;
		this.max = max;
	}

	public Fetch<?,?> getChildren() {
		return children;
	}

	public DMPropertyHolder<?,?,?> getProperty() {
		return property;
	}
	
	public int getMax() {
		return max;
	}
	
	public boolean getNotify() {
		return notify;
	}
	

	public PropertyFetch merge(PropertyFetch other) {
		if (equals(other))
			return this;
		
		Fetch<?, ?> newChildren;
		
		if (children == null)
			newChildren = other.children;
		else if (other.children == null)
			newChildren = children;
		else
			newChildren = children.merge(other.children);
			
		boolean newNotify = notify || other.notify;
		int newMax = Math.max(max, other.max);
			
		return new PropertyFetch(property, newChildren, newNotify, newMax);
	}
	
	public int compareTo(PropertyFetch other) {
		return property.compareTo(other.property);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PropertyFetch))
			return false;
		
		PropertyFetch other = (PropertyFetch)o;
		if (property != other.property)
			return false;
		
		if (notify != other.notify)
			return false;
		
		if (max != other.max)
			return false;
		
		if ((children == null && other.children != null) ||
			(children != null && !children.equals(other.children)))
			return false;
		
		return true;
	}
	
	@Override
	public int hashCode() {
		int value = notify ? 0 : 113;
		value += property.hashCode() * 17;
		if (children != null)
			value += children.hashCode() * 23;
		
		return value;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		b.append(property.getPropertyId());
		
		if (max >= 0 || !notify)
			b.append('(');
		
		if (max >= 0) {
			b.append("max=");
			b.append(max);
			if (!notify)
				b.append(",");
		}
		if (!notify)
			b.append("notify=false");
		if (max >= 0 || !notify)
			b.append(')');
		
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
