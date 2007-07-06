package com.dumbhippo.dm.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.ClientNotificationSet;
import com.dumbhippo.dm.DMClient;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.NotCachedException;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.schema.DMClassHolder;

public class DMStore {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(DMStore.class);
	
	public Map<StoreKey<?,?>, StoreNode<?,?>> nodes = new HashMap<StoreKey<?,?>, StoreNode<?,?>>();
	
	@SuppressWarnings("unchecked")
	private <K, T extends DMObject<K>> StoreNode<K,T> getNode(StoreKey<K,T> storeKey) {
		synchronized(nodes) {
			return (StoreNode<K,T>) nodes.get(storeKey);
		}
	}
	
	private <K, T extends DMObject<K>> StoreNode<K,T> getNode(DMClassHolder<K,T> classHolder, K key) {
		synchronized(nodes) {
			StoreKey<K,T> storeKey = new StoreKey<K,T>(classHolder, key);
			return getNode(storeKey);
		}
	}
	
	private <K, T extends DMObject<K>> StoreNode<K,T> ensureNode(StoreKey<K,T> storeKey) {
		synchronized(nodes) {
			StoreNode<K,T> node = getNode(storeKey);
			if (node != null)
				return node;
			
			K key = storeKey.getKey();
			if (key instanceof DMKey) {
				@SuppressWarnings("unchecked")
				K clonedKey = (K)((DMKey)key).clone();
	
				key = clonedKey;
			}
	
			node = new StoreNode<K,T>(storeKey.getClassHolder(), key);
			
			nodes.put(node, node);
			
			return node;
		}
		
	}
	
	private <K, T extends DMObject<K>> StoreNode<K,T> ensureNode(DMClassHolder<K,T> classHolder, K key) {
		StoreKey<K,T> storeKey = new StoreKey<K,T>(classHolder, key);
		
		return ensureNode(storeKey);
	}
	
	private void evictNode(StoreNode<?,?> node) {
		synchronized(nodes) {
			StoreNode<?,?> current = nodes.get(node);
			if (current == node)
				nodes.remove(node);
		}
	}
	
	public <K, T extends DMObject<K>> Object fetch(StoreKey<K,T> key, int propertyIndex) throws NotCachedException {
		StoreNode<K,T> node = getNode(key);
		if (node == null)
			throw new NotCachedException();
		
		// Do we have to worry about evication? The sequence, while fetching data from the client:
		//
		// 0. The client allocates a serial
		// 1. The client registers for notification on key
		// 2. The node for the key is evicted
		// 3. The client fetches data from the key
		// 4. The client returns the data using with the originally allocated serial 
		//
		// Is fine, because the eviction notice will have a high serial (and thus be delivered after)
		// the returned data. So, no, eviction isn't a problem. 
		
		return node.fetch(propertyIndex);
	}

	public <K, T extends DMObject<K>> boolean checkCached(StoreKey<K,T> key) {
		StoreNode<K,T> node = getNode(key);
		return node != null;
	}

	public <K, T extends DMObject<K>> void store(StoreKey<K,T> key, int propertyIndex, Object value, long timestamp) {
		StoreNode<K,T> node = ensureNode(key);
		
		// It's not a problem if the node is evicted; the data just won't be stored this time around
		node.store(propertyIndex, value, timestamp);
	}

	public <K, T extends DMObject<K>> void invalidate(DMClassHolder<K,T> classHolder, K key, int propertyIndex, long timestamp) {
		// We need to make sure that we store the timestamp to deal with in-flight 
		// fetch-NotCached / store pairs. So, we ensureNode() and, if after invalidating, the
		// node is evicted, then we try again. Note that we still don't handle everything - the
		// case where:
		//
		//   Thread A fetches, node is not there
		//   Node is invalidated, creating the node
		//   Node is evicted
		//   Thread A stores the new data
		//
		// So we need to make sure that nodes that are newly created are not evicted until
		// after any reasonable succesful fetch should return. Simplest way to do this is
		// with an eviction policy that doesn't evict for a minimum of 30 minutes or so.
		// (Note that eviction is never forced; if data changes we invalidate, not evict.)
		// There is still a small danger of an anomolous fetch taking arbitrarily long
		// if there is a glacially responding external server, say. 
		//
		StoreNode<K,T> node;
		do {
			node = ensureNode(classHolder, key);
			node.invalidate(propertyIndex, timestamp);
		} while (node.isEvicted());
	}

	public <K, T extends DMObject<K>>  void resolveNotifications(DMClassHolder<K,T> classHolder, K key, long propertyMask, ClientNotificationSet result) {
		StoreNode<K,T> node = getNode(classHolder, key);
		if (node == null)
			return;
		
		node.resolveNotifications(propertyMask, result);
	}
	
	public StoreClient openClient(DMClient client) {
		return new StoreClient(client);
	}
	
	public <K, T extends DMObject<K>> Fetch<K,? super T> addRegistration(DMClassHolder<K,T> classHolder, K key, StoreClient client, Fetch<K,? super T> fetch) {
		StoreNode<K,T> node;
		Registration<K,T> registration;
		
		// A null return from createRegistration means the node was evicted before we could add 
		// the registration, so try again to get a new node. If the node is evicted after our 
		// registration is added,that's OK, since a notification of the eviction will be sent to
		// the client, and it can recover.
		do {
			node = ensureNode(classHolder, key);
			registration = node.createRegistration(client);
		} while (registration == null);
		
		Fetch<K,? super T> oldFetch = registration.addFetch(fetch);
		
		// If the client is already closed, then we need to remove the registration we added,
		// since it won't be cleaned up when the client is closed.
		if (!client.addRegistration(registration))
			node.removeRegistration(registration);
		
		return oldFetch;
	}
	
	public <K, T extends DMObject<K>> void removeRegistration(DMClassHolder<K,T> classHolder, K key, StoreClient client) {
		StoreNode<K,T> node = getNode(classHolder, key);
		if (node == null)
			return;
		
		Registration<K,T> registration = node.removeRegistration(client);
		if (registration == null)
			return;
		
		// This will only remove this registration and ignore any subsequently added for the same key.
		client.removeRegistration(registration);
	}
	
	public void closeClient(StoreClient client) {
		// Calling client.close() atomically marks the client as closed and returns all
		// registrations at that point of time.
		for (Registration<?,?> registration : client.close()) {
			registration.removeFromNode();
		}
	}
	
	public <K, T extends DMObject<K>> void evict(DMClassHolder<K,T> classHolder, K key) {
		StoreNode<K,T> node = getNode(classHolder, key);
		if (node == null)
			return;
		
		Collection<Registration<K,T>> registrationsAtEviction = node.markEvicted();
		evictNode(node);
		
		// client.removeRegistration is careful not to remove any other registration
		// subsequently added for the same StoreKey.
		for (Registration<K,T> registration : registrationsAtEviction) {
			StoreClient client = registration.getClient();
			
			client.removeRegistration(registration);
			
			// Note that allocating the serial at this point can allow late 
			// eviction notices to happen. 
			//
			// 1. Node is evicted
			// 2. Node is resurrected by a different thread, serial allocated
			// 3. Eviction notice is sent with a later serial
			//
			// Which will cause clients to think that their registration was
			// evicted when it wasn't. That's pretty harmless though - if the 
			// client recovers as if the eviction really happened, it will 
			// be a no-op, and if the client choooses not to recover, it should
			// be able to handle the unexpected notifications later... clients
			// should never count on *not* getting notifications.
			//
			// Allocating the serial before the eviction is worse, since the
			// eviction happens but the client doesn't get notified. And allocating
			// serials for all the clients atomically with the eviction would
			// require some earth-spanning reader-writer lock which we don't
			// want.
			
			client.notifyEviction(classHolder, key, client.allocateSerial());
			registration.getClient().removeRegistration(registration);
		}
	}
}
