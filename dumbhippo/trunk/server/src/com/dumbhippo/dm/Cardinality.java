package com.dumbhippo.dm;

public enum Cardinality {
	ZERO_ONE("01"),
	ONE("1"),
	ANY("N");
	
	private String value;

	private Cardinality(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
}
