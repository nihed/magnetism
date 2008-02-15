package com.dumbhippo.dm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.fetch.BoundFetch;
import com.dumbhippo.dm.store.StoreClient;
import com.dumbhippo.dm.store.StoreKey;

/**
 * A ClientNotificationSet keeps track of which notifications need to be sent to which
 * clients. It is the result of resolving a {@link ChangeNotificationSet} after the
 * end of a transaction.
 * 
 * @author otaylor
 */
public class ClientNotificationSet {
	@SuppressWarnings("unused")
	static final private Logger logger = GlobalSetup.getLogger(ClientNotificationSet.class);
	
	private Map<StoreClient, ClientNotification> notifications;

	private ClientNotification getNotification(StoreClient client) {
		ClientNotification notification = null;
		
		if (notifications == null)
			notifications = new HashMap<StoreClient, ClientNotification>();
		else
			notification = notifications.get(client);
		
		if (notification == null) {
			notification = new ClientNotification(client);
			notifications.put(client, notification);
		}

		return notification;
	}
	
	public <K, T extends DMObject<K>> void addNotification(StoreClient client, StoreKey<K,T> key, BoundFetch<K,? super T> fetch, long propertyMask, BoundFetch<?,?>[] childFetches, int[] maxes) {
		ClientNotification notification = getNotification(client);
		notification.addObjectProperties(key, fetch, propertyMask, childFetches, maxes);
	}
	
	public <K, T extends DMObject<K>> void addDeletion(StoreClient client, StoreKey<K,T> key) {
		ClientNotification notification = getNotification(client);
		notification.addDeletion(key);
	}
	
	public Collection<ClientNotification> getNotifications() {
		if (notifications != null)
			return notifications.values();
		else
			return Collections.emptyList();
	}
	
	@Override
	public String toString() {
		if (notifications != null)
			return notifications.values().toString();
		else 
			return "[]";
	}
}
