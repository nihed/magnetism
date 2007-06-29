package com.dumbhippo.persistence;

import junit.framework.TestCase;

import com.dumbhippo.server.TestUtils;

public class GuidPersistableTest extends TestCase {
	
	public void testPersistables() {
		Person p1 = new User();
		Person p2 = new Contact();
		Person p3 = new User();
		EmailResource e1 = new EmailResource("blah@example.com");
		EmailResource e2 = new EmailResource("blah2@example.com");		
		TestUtils.testEqualsImplementation(p1, p2, p3, e1, e2);
	}

}
