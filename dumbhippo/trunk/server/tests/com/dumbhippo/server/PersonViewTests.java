package com.dumbhippo.server;

import com.dumbhippo.persistence.Storage.SessionWrapper;

public class PersonViewTests extends SpiderUsingTest {
	
	public void testGetEmail() {
		SessionWrapper sess = getSession();
		sess.beginTransaction();
		
		Person p = getTestPerson1();
		
		sess.commitCloseBeginTransaction();
		
		PersonView systemView = spider.getViewpoint(null, p);
		PersonView selfView = spider.getViewpoint(p, p);
		
		EmailResource email = systemView.getEmail();
		assertNotNull(email);
		assertEquals(email.getEmail(), testPerson1EmailString);
		email = selfView.getEmail();
		assertNotNull(email);
		assertEquals(email.getEmail(), testPerson1EmailString);		
		
		sess.commitTransaction();
	}
	
	public void testGetHumanReadableName() {
		SessionWrapper sess = getSession();
		sess.beginTransaction();
		
		Person p = getTestPerson1();
		
		sess.commitCloseBeginTransaction();
		
		PersonView systemView = spider.getViewpoint(null, p);
		PersonView selfView = spider.getViewpoint(p, p);
		
		String readable = systemView.getHumanReadableName();
		assertNotNull(readable);
		readable = selfView.getHumanReadableName();
		assertNotNull(readable);		
		
		sess.commitTransaction();
	}
	
	public void testGetEmail2() {
		SessionWrapper sess = getSession();
		sess.beginTransaction();
		
		Person p1 = getTestPerson1();
		Person p2 = getTestPerson2();
		
		sess.commitCloseBeginTransaction();
		
		PersonView p2ViewP1 = spider.getViewpoint(p2, p1);
		
		EmailResource email = p2ViewP1.getEmail();
		assertNotNull(email);
		assertEquals(email.getEmail(), testPerson1EmailString);	
		String readable = p2ViewP1.getHumanReadableName();
		assertNotNull(readable);		
		
		sess.commitTransaction();
	}	
}
