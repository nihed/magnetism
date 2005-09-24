package com.dumbhippo.server;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;

public class IdentitySpiderBeanTest extends StorageUsingTest {

	public void testAddPersonWithEmail() {
		final String email = "test@example.com";
		
		IdentitySpiderBean tarantula = new IdentitySpiderBean();
		
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		sess.beginTransaction();
		
		Person p = tarantula.lookupPersonByEmail(email);
		assertNull(p);
		
		p = tarantula.addPersonWithEmail(email);
		Guid guid = p.getGuid();
		
		sess.commitCloseBeginTransaction();
		
		p = tarantula.lookupPersonByEmail(email);
		assertNotNull(p);
		assertEquals(p.getGuid(), guid);
	}
}
