package com.dumbhippo.dm.fetch;

public class FetchAttribute {
	private FetchAttributeType type;
	private Object value;
	
	public FetchAttribute(FetchAttributeType type, Object value) {
		this.type = type;
		this.value = value;
	}

	public FetchAttributeType getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return type.getLowerName() + "=" + value;
	}
}
