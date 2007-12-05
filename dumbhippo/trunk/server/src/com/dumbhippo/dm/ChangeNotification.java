package com.dumbhippo.dm;

import java.io.Serializable;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.schema.FeedPropertyHolder;

/**
 * ChangeNotification represents pending notifications for a single resource.
 * 
 * @param <K>
 * @param <T>
 */
public class ChangeNotification<K, T extends DMObject<K>> implements Serializable {
	private static final long serialVersionUID = 3460039660076438219L;

	private static Logger logger = GlobalSetup.getLogger(ChangeNotification.class);

	private DMClassHolder<K, T> classHolder;
	private K key;
	private long propertyMask; // bitset
	private ClientMatcher matcher;

	private long feedTimestamps[];

	public ChangeNotification(DMClassHolder<K,T> classHolder, K key) {
		this.classHolder = classHolder;
		this.key = key;
	}
	
	public ChangeNotification(DMClassHolder<K,T> classHolder, K key, ClientMatcher matcher) {
		this.classHolder = classHolder;
		this.key = key;
		this.matcher = matcher;
	}
	
	public void addProperty(int propertyIndex) {
		this.propertyMask |= 1 << propertyIndex;
	}
	
	public void addProperty(String propertyName) {
		int propertyIndex = classHolder.getPropertyIndex(propertyName);
		if (propertyIndex < 0)
			throw new RuntimeException("Class " + classHolder.getBaseClass().getName() + " has no property " + propertyName);
		
		DMPropertyHolder<K,T,?> property = classHolder.getProperty(propertyIndex);
		if (property instanceof FeedPropertyHolder)
			throw new RuntimeException("For feed-valued-properties, you must use session.feedChanged()");
		
		addProperty(propertyIndex);
	}
	
	public void addFeedProperty(String propertyName, long itemTimestamp) {
		int propertyIndex = classHolder.getPropertyIndex(propertyName);
		if (propertyIndex < 0)
			throw new RuntimeException("Class " + classHolder.getBaseClass().getName() + " has no property " + propertyName);
		
		addProperty(propertyIndex);
		
		DMPropertyHolder<K,T,?> property = classHolder.getProperty(propertyIndex);
		if (!(property instanceof FeedPropertyHolder))
			throw new RuntimeException("session.feedChanged() for a non-feed-valued property");

		int feedPropertyIndex = classHolder.getFeedPropertyIndex(property.getName());
		
		if (feedTimestamps == null) {
			feedTimestamps = new long[classHolder.getFeedPropertiesCount()];
			for (int i = 0; i < feedTimestamps.length; i++)
				feedTimestamps[i] = Long.MAX_VALUE; 
		}
		
		if (feedTimestamps[feedPropertyIndex] > itemTimestamp)
			feedTimestamps[feedPropertyIndex] = itemTimestamp;
	}
	
	public void invalidate(DataModel model, long timestamp) {
		long v = propertyMask;
		int propertyIndex = 0;
		while (v != 0) {
			if ((v & 1) != 0) {
				DMPropertyHolder<K, T, ?> property = classHolder.getProperty(propertyIndex);
				
				if (property instanceof FeedPropertyHolder) {
					int feedPropertyIndex = classHolder.getFeedPropertyIndex(property.getName());
					model.getStore().invalidateFeed(classHolder, key, propertyIndex, timestamp, feedTimestamps[feedPropertyIndex]);
				} else
					model.getStore().invalidate(classHolder, key, propertyIndex, timestamp);
				
				logger.debug("Invalidated {}#{}.{}", new Object[] { 
						classHolder.getBaseClass().getSimpleName(),
						key, 
						property.getName() });
			}
				
			propertyIndex++;
			v >>= 1;
		}
	}

	public void resolveNotifications(DataModel model, ClientNotificationSet result) {
		model.getStore().resolveNotifications(classHolder, key, propertyMask, result, matcher);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ChangeNotification))
			return false;
		
		ChangeNotification<?,?> other = (ChangeNotification<?,?>)o;
		
		return classHolder == other.classHolder && key.equals(other.key);
	}
	
	@Override
	public int hashCode() {
		return 11 * classHolder.hashCode() + 17 * key.hashCode();
	}
	
	@Override
	public String toString() {
		return classHolder.getBaseClass().getSimpleName() + "#" + key.toString();
	}
}
