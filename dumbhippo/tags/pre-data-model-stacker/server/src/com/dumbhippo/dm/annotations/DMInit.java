package com.dumbhippo.dm.annotations;

/**
 * Marks a method of a DMO class as an initialization function for a group of properties 
 */
public @interface DMInit {
	/**
	 * The group of properties that this initialization function is for.  
	 */
	int group();
	
	/**
	 * Whether to call the main init() function before this initialization function.
	 */
	boolean initMain() default true;
}
