package com.dumbhippo.dm.filter;

public enum ConditionType {
	THIS("this", 0),
	ALL("all", 1),
	ANY("any", 1),
	ITEM("item", 2);
	
	private String lowerName;
	private int processingPhase;
	
	private ConditionType(String lowerName, int processingPhase) {
		this.lowerName = lowerName;
		this.processingPhase = processingPhase;
	}
	
	public String getLowerName() {
		return this.lowerName;
	}
	
	public int getProcessingPhase() {
		return processingPhase;
	}
}
