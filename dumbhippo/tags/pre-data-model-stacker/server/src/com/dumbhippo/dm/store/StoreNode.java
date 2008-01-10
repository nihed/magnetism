package com.dumbhippo.dm.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.CachedFeed;
import com.dumbhippo.dm.ClientMatcher;
import com.dumbhippo.dm.ClientNotificationSet;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.NotCachedException;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.DMPropertyHolder;

public class StoreNode<K,T extends DMObject<K>> extends StoreKey<K,T> {
	@SuppressWarnings("unused")
	static final private Logger logger = GlobalSetup.getLogger(StoreNode.class);

	private static final Object nil = new Boolean(false);

	private long timestamp = -1;
	private Object[] properties;
	private List<Registration<K,T>> registrations;
	private FeedLog[] feedLogs;
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
	
	public synchronized CachedFeed<?> getOrCreateCachedFeed(int propertyIndex, long timestamp) {
		if (this.timestamp > timestamp)
			return null;
		
		if (properties[propertyIndex] == null) {
			// Actual key type is irrelevant
			properties[propertyIndex] = new CachedFeed<Object>();
		}
		
		return (CachedFeed<?>)properties[propertyIndex];
	}

	public synchronized void invalidate(int propertyIndex, long timestamp) {
		this.timestamp = timestamp;
		properties[propertyIndex] = null;
	}
	
	public synchronized void invalidateFeed(int propertyIndex, long txTimestamp, long itemTimestamp) {
		invalidate(propertyIndex, timestamp);
		
		DMPropertyHolder<K, T, ?> property = classHolder.getProperty(propertyIndex);
		int feedPropertyIndex = classHolder.getFeedPropertyIndex(property.getName());
		
		if (feedLogs == null)
			feedLogs = new FeedLog[classHolder.getFeedPropertiesCount()];
		
		if (feedLogs[feedPropertyIndex] == null)
			feedLogs[feedPropertyIndex] = new FeedLog();
		
		feedLogs[feedPropertyIndex].addEntry(txTimestamp, itemTimestamp);
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
		
		registrations.remove(registration);
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
	
	public synchronized void resolveNotifications(long properties, ClientNotificationSet result, ClientMatcher matcher) {
		if (evicted || registrations == null)
			return;
		
		for (Registration<K,T> registration : registrations) {
			if (matcher != null && !matcher.matches(registration.getClient().getWrappedClient())) {
//				logger.debug("Skipping notification on {} because ClientMatcher {} doesn't match {}", 
//						     new Object[] { this, matcher, registration.getClient() });
				continue;
			}
			
//			logger.debug("For {}/{}, comparing notified properties {} to registered properties {}",
//						 new Object[] { this, registration.getClient(), properties, registration.getFetch().getFetchString(classHolder) });
			
			registration.getFetch().resolveNotifications(registration.getClient(), this, properties, result);
		}
	}

	public FeedLog getFeedLog(int feedPropertyIndex) {
		if (feedLogs == null)
			return null;
		else
			return feedLogs[feedPropertyIndex];
	}
	
	// Returning a StoreKey rather than a StoreNode is perhaps a little dubious, but we want cloning 
	// a StoreKey to give us something we can store in the cache (for properties of type StoreKey)
	@Override
	public StoreKey<K,T> clone() {
		if (key instanceof DMKey) {
			@SuppressWarnings("unchecked")
			K clonedKey = (K)((DMKey)key).clone();
			return new StoreKey<K,T>(classHolder, clonedKey);
		} else {
			return new StoreKey<K,T>(classHolder, key);
		}
	}
	
}
