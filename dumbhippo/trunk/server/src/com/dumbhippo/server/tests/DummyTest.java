package com.dumbhippo.server.tests;

import com.dumbhippo.server.Dummy;

import junit.framework.TestCase;

public class DummyTest extends TestCase {

	public void testMultiplyByThree() {
		Dummy dummy = new Dummy();
		int v = dummy.multiplyByThree(3);
		assertEquals(v, 9);
	}

}
