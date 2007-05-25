package com.dumbhippo.dm.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.dumbhippo.dm.DMClient;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;

public class StoreClient implements DMClient {
	private DMClient client;
	private Map<StoreKey, Registration<?,?>> registrations = new HashMap<StoreKey, Registration<?,?>>();
	private AtomicLong nextSerial = new AtomicLong(-1);
	private boolean closed;
	
	public StoreClient(DMClient client) {
		this.client = client;
	}

	public long allocateSerial() {
		return nextSerial.addAndGet(1);
	}

	public boolean addRegistration(Registration registration) {
		synchronized (registrations) {
			if (closed)
				return false;
			
			registrations.put(registration.getNode(), registration);
			return true;
		}
	}
	
	public void removeRegistration(Registration registration) {
		synchronized(registrations) {
			StoreKey<?,?> key = registration.getNode();
			
			if (closed)
				return;
			
			Registration current = registrations.get(key);
			if (current == registration)
				registrations.remove(key);
		}
	}
	
	public Collection<Registration<?,?>> close() {
		synchronized (registrations) {
			closed = true;
			return registrations.values();
		}
	}
	
	public DMViewpoint createViewpoint() {
		return client.createViewpoint();
	}
	
	public FetchVisitor beginNotification() {
		return client.beginNotification();
	}

	public void endNotification(FetchVisitor visitor, long serial) {
		client.endNotification(visitor, serial);
	}

	public <K, T extends DMObject<K>> void notifyEviction(DMClassHolder<K,T> classHolder, K key, long serial) {
		client.notifyEviction(classHolder, key, serial);
	}

	public void nullNotification(long serial) {
		client.nullNotification(serial);
	}
	
	@Override
	public String toString() {
		return "StoreClient(" + client + ")"; 
	}
}
