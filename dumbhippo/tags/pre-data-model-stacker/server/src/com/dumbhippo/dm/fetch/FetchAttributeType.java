package com.dumbhippo.dm.fetch;

public enum FetchAttributeType {
	MAX("max"),
	NOTIFY("notify");
	
	private String lowerName;
	
	FetchAttributeType(String lowerName) {
		this.lowerName = lowerName;
	}
	
	public String getLowerName() {
		return lowerName;
	}
}
