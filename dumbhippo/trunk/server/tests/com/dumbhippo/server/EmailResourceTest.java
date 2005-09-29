package com.dumbhippo.server;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Storage;

public class EmailResourceTest extends StorageUsingTest {

	/*
	 * Class under test for void Resource()
	 */
	public void testResource() {
		Storage.SessionWrapper session = getSession();
		EmailResource email = new EmailResource("foo@bar.com");
		
		Guid guid = email.getGuid();
		String guidStr = email.getId();
		assertEquals(guidStr.length(), Guid.STRING_LENGTH);

		session.beginTransaction();
		
		session.getSession().saveOrUpdate(email);
		
		session.commitCloseBeginTransaction();
		
		EmailResource loadedEmail =
			(EmailResource) session.loadFromGuid(EmailResource.class,
				guid);
		
		assertEquals(guid, loadedEmail.getGuid());
		assertEquals(loadedEmail.getEmail(), email.getEmail()); 
	}
}
