package com.dumbhippo.dm;

/**
 * 
 * @author otaylor
 */
public enum Cardinality {
	/** 0 or one values */
	ZERO_ONE("01"),
	/** Exactly one value */
	ONE("1"),
	/** Any number of values */
	ANY("N");
	
	private String value;

	private Cardinality(String value) {
		this.value = value;
	}
	
	/**
	 * Get the string form used in the XML wire protocol 
	 * 
	 * @return the string form.
	 */
	public String getValue() {
		return value;
	}
}
