package com.dumbhippo.server;

public class SpiderUsingTest extends StorageUsingTest {
	protected final String testPerson1Email = "test@example.com";
	protected final String testPerson2Email = "test2@example.net";	
	protected IdentitySpiderBean spider;
	protected Person testPerson1;
	protected Person testPerson2;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		spider = new IdentitySpiderBean();
	}

	@Override
	protected void tearDown() throws Exception {
		spider = null;
		testPerson1 = null;
		testPerson2 = null;
		super.tearDown();
	}
	
	protected Person getTestPerson1() {
		if (testPerson1 == null)
			testPerson1 = spider.addPersonWithEmail(testPerson1Email);
		return testPerson1;
	}
	
	protected Person getTestPerson2() {
		if (testPerson2 == null)
			testPerson2 = spider.addPersonWithEmail(testPerson2Email);
		return testPerson2;
	}

}
