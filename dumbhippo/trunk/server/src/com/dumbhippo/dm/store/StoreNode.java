package com.dumbhippo.dm.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.dumbhippo.dm.ClientNotificationSet;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.NotCachedException;
import com.dumbhippo.dm.schema.DMClassHolder;

public class StoreNode<K,T extends DMObject<K>> extends StoreKey<K,T> {
	private static Object nil = new Boolean(false);

	private long timestamp = -1;
	private Object[] properties;
	private List<Registration<K,T>> registrations;
	private boolean evicted;

	StoreNode(DMClassHolder<K,T> classHolder, K key) {
		super(classHolder, key);
		
		this.properties = new Object[classHolder.getPropertyCount()];
	}
	
	public synchronized Object fetch(int propertyIndex) throws NotCachedException {
		Object value = properties[propertyIndex];
		if (value == null)
			throw new NotCachedException();
		
		return value != nil ? value : null;
	}
	
	public synchronized void store(int propertyIndex, Object value, long timestamp) {
		if (this.timestamp > timestamp)
			return;
		
		properties[propertyIndex] = value != null ? value : nil;
	}
	
	public synchronized void invalidate(int propertyIndex, long timestamp) {
		this.timestamp = timestamp;
		properties[propertyIndex] = null;
	}
	
	public synchronized Collection<Registration<K,T>> markEvicted() {
		evicted = true;
		return registrations;
	}
	
	public synchronized boolean isEvicted() {
		return evicted;
	}
	
	public synchronized Registration<K,T> createRegistration(StoreClient client) {
		if (evicted)
			return null;
		
		if (registrations == null)
			registrations = new ArrayList<Registration<K,T>>();
		
		for (Registration<K,T> registration : registrations) {
			if (registration.getClient() == client) 
				return registration;
		}
		
		Registration<K,T> newRegistration = new Registration<K,T>(this, client);
		registrations.add(newRegistration);
		
		return newRegistration;
	}

	public synchronized void removeRegistration(Registration<K,T> registration) {
		if (evicted || registrations == null)
			return;
		
		registrations.remove(registrations);
	}
	
	public synchronized Registration<K,T> removeRegistration(StoreClient client) {
		if (evicted || registrations == null)
			return null;
		
		Iterator<Registration<K,T>> iter = registrations.iterator();
		while (iter.hasNext()) {
			Registration<K,T> registration = iter.next();
			if (registration.getClient() == client) { 
				iter.remove();
				return registration;
			}
		}
		
		return null; 
	}
	
	public synchronized void resolveNotifications(long properties, ClientNotificationSet result) {
		if (evicted || registrations == null)
			return;
		
		for (Registration<K,T> registration : registrations)
			registration.getFetch().resolveNotifications(registration.getClient(), this, properties, result); 
	}
}
