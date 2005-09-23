package com.dumbhippo.server;

public class ResourceTest extends StorageUsingTest {

	/*
	 * Class under test for void Resource()
	 */
	public void testResource() {
		Storage.SessionWrapper session = Storage.getGlobalPerThreadSession(); 
		
		EmailResource email = new EmailResource();
		
		assertEquals(email.getId().length(), Guid.STRING_LENGTH);
		
		System.err.println("Created new resource '" + email.getId() + "'");
		
		email.setEmail("foo@bar.com");
		
		session.beginTransaction();
		
		session.getSession().saveOrUpdate(email);
		
		session.commitCloseBeginTransaction();
		
		EmailResource loadedEmail =
			(EmailResource) session.loadFromGuid(EmailResource.class,
				email.getGuid());
		
		System.err.println("Loaded resource " + loadedEmail.getId());
		
		assertTrue (loadedEmail.equals(email)); 
	}
}
