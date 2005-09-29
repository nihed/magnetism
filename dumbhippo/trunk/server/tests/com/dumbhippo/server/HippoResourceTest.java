package com.dumbhippo.server;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Storage;

public class HippoResourceTest extends StorageUsingTest {

	/*
	 * Class under test for void Resource()
	 */
	public void testResource() {
		Storage.SessionWrapper session = getSession();
		
		HippoResource account = new HippoResource();
		
		Guid guid = account.getGuid();

		session.beginTransaction();
		
		session.getSession().saveOrUpdate(account);
		
		session.commitCloseBeginTransaction();
		
		HippoResource loadedAccount =
			(HippoResource) session.loadFromGuid(HippoResource.class,
				guid);
		
		assertEquals(guid, loadedAccount.getGuid()); 
	}
}
