package com.dumbhippo.server;

public class SpiderUsingTest extends StorageUsingTest {
	protected final String testPerson1EmailString = "test@example.com";
	protected final String testPerson2EmailString = "test2@example.net";	
	protected IdentitySpiderBean spider;
	protected Person testPerson1;
	protected Person testPerson2;
	private EmailResource testPerson1Email;
	private EmailResource testPerson2Email;
	private EmailResource noPersonEmail;
	
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
			testPerson1 = spider.addPersonWithEmail(getTestPerson1Email());
		return testPerson1;
	}
	
	protected Person getTestPerson2() {
		if (testPerson2 == null)
			testPerson2 = spider.addPersonWithEmail(getTestPerson2Email());
		return testPerson2;
	}

	protected EmailResource getTestPerson1Email() {
		if (testPerson1Email == null)
			testPerson1Email = spider.getEmail(testPerson1EmailString);
		return testPerson1Email;
	}

	protected EmailResource getTestPerson2Email() {
		if (testPerson2Email == null)
			testPerson2Email = spider.getEmail(testPerson2EmailString);
		return testPerson2Email;
	}
	
	protected EmailResource getNoPersonEmail() {
		if (noPersonEmail == null)
			noPersonEmail = spider.getEmail("notestperson@example.com");
		return noPersonEmail;
	}
}
