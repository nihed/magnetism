package com.dumbhippo.imbot.test;


import junit.framework.TestCase;

/**
 * Class with assorted utilities for testing
 * @author hp
 *
 */
public class TestUtils extends TestCase {
	
	public void testDummy() { 
		// junit wants at least one test method per class in this directory,
		// adding this bogus method is easier than hacking ant or something
	}
	
	/** 
	 * Pass this function a bunch of sample objects of your class;
	 * subclasses are OK as long as you expect them to support equals()
	 * and hashCode() correctly. Function tests reflexivity, transitivity,
	 * and consistency of equals() with hashCode().
	 * 
	 * @param sampleObjects
	 */
	public static void testEqualsImplementation(Object... sampleObjects) {
		for (Object a : sampleObjects) {
			TestCase.assertTrue(a != null); // bug in usage of this utility function
			
			// must not be equal to null
			TestCase.assertTrue (!a.equals(null));
			
			// must be equal to self (reflexivity)
			TestCase.assertTrue(a.equals(a));
			
			// hash code must always return same value
			TestCase.assertTrue(a.hashCode() == a.hashCode());
			
			for (Object b : sampleObjects) {
				
				// symmetry, objects must agree on equality
				if (a.equals(b)) {
					TestCase.assertTrue(b.equals(a));
					
					// must have same hash code if they are equal
					TestCase.assertTrue(a.hashCode() == b.hashCode());
					
				} else {
					TestCase.assertTrue(!b.equals(a));
				}
				
			}
		}
	}
}
