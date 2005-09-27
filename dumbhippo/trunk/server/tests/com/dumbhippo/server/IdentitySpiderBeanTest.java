package com.dumbhippo.server;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;

public class IdentitySpiderBeanTest extends SpiderUsingTest {

	public void testAddPersonWithEmail() {
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		sess.beginTransaction();
		
		Person p = spider.lookupPersonByEmail(testPerson1Email);
		assertNull(p);
		
		p = getTestPerson1();
		Guid guid = p.getGuid();
		
		sess.commitCloseBeginTransaction();
		
		p = spider.lookupPersonByEmail(testPerson1Email);
		assertNotNull(p);
		assertEquals(p.getGuid(), guid);
		
		sess.commitTransaction();
	}
	
	public void testLookupPersonEmail() {
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		sess.beginTransaction();
		
		Person p = getTestPerson1();
		
		sess.commitCloseBeginTransaction();
		
		String email = spider.getEmailAddress(p);
		assertNotNull(email);
		assertEquals(email, testPerson1Email);
		email = spider.getEmailAddress(p, p);
		assertNotNull(email);
		assertEquals(email, testPerson1Email);
		
		sess.commitTransaction();
	}	
}
