package com.dumbhippo.server;

import junit.framework.TestCase;

public class BasicTests extends TestCase {
	public void testAssertionsEnabled() {
		try {
			assert (1+1 == 3);
			assertTrue(false);
		} catch (AssertionError e) {
		} 
	}
}
