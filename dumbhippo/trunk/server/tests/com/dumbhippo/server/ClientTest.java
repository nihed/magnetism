package com.dumbhippo.server;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.Storage;

public class ClientTest extends StorageUsingTest {

	public void testClient() {
		Storage.SessionWrapper session = getSession();
		
		Client client = new Client();
		
		session.beginTransaction();
		
		session.getSession().saveOrUpdate(client);
		
		session.commitCloseBeginTransaction();
		
		long id = client.getId();
		
		Client loadedClient =
			(Client) session.loadFromId(Client.class,
				id);
		
		assertEquals(id, loadedClient.getId());
		assertEquals(client.getAuthKey(), loadedClient.getAuthKey());
		assertEquals(client.getLastUsed(), loadedClient.getLastUsed());
		assertEquals(client.getName(), loadedClient.getName());
	}
}
