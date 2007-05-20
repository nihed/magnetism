package com.dumbhippo.dm.fetch;

import com.dumbhippo.dm.DMPropertyHolder;


public final class PropertyFetch implements Comparable<PropertyFetch> {
	private DMPropertyHolder property;
	private Fetch children;
	private boolean notify;
	
	public PropertyFetch(DMPropertyHolder property, Fetch children, boolean notify) {
		this.property = property;
		this.children = children;
		this.notify = notify;
	}

	public Fetch getChildren() {
		return children;
	}

	public DMPropertyHolder getProperty() {
		return property;
	}
	
	public boolean getNotify() {
		return notify;
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
		
		if (!notify)
			b.append("(notify=false)");
		
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
