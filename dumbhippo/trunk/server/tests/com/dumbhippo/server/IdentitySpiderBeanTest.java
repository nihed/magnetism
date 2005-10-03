package com.dumbhippo.server;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.PersonView;
import com.dumbhippo.persistence.Storage.SessionWrapper;

public class IdentitySpiderBeanTest extends SpiderUsingTest {

	public void testTheMan() {
		SessionWrapper sess = getSession();
		sess.beginTransaction();
		
		Person man = spider.getTheMan();
		assertNotNull(man);
		String manName = new PersonView(null, man).getHumanReadableName();
		assertNotNull(manName);
		assertTrue(manName.contains("@"));
		
		sess.commitTransaction();
	}
	
	public void testAddPersonWithEmail() {
		SessionWrapper sess = getSession();
		sess.beginTransaction();
		
		Person p = spider.lookupPersonByEmail(getTestPerson1Email());
		assertNull(p);
		
		p = getTestPerson1();
		Guid guid = p.getGuid();
		
		sess.commitCloseBeginTransaction();
		
		p = spider.lookupPersonByEmail(getTestPerson1Email());
		assertNotNull(p);
		assertEquals(p.getGuid(), guid);
		
		sess.commitTransaction();
	}

}
