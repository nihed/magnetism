package com.dumbhippo.persistence;

public enum StackFilterFlags {
	FEED(1);
	
	private int value;
	
	StackFilterFlags(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}
