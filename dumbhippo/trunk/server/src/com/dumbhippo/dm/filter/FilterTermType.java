package com.dumbhippo.dm.filter;

public enum FilterTermType {
	KEY("key"),
	ITEM("item"),
	ALL("all"),
	ANY("any");
	
	String lowerName;
	
	FilterTermType(String lowerName) {
		this.lowerName = lowerName;
	}
	
	String getLowerName() {
		return this.lowerName;
	}
}
