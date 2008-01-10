package com.dumbhippo.dm;

import junit.framework.TestCase;

public abstract class AbstractSupportedTests  extends TestCase {
	TestSupport support;
	
	@Override
	protected void setUp()  {
		support = TestSupport.getInstance();
	}
	
	@Override
	protected void tearDown() {
		support = null;
	}
}
