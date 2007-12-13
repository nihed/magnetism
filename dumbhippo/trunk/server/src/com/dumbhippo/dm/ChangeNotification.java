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
 * Note that this class must be serializable, because we send it over JMS when
 * broadcasting changes. That's why we store Class<T> rather than DMClassHolder<K,T>,
 * even though it means we have to look up the DMClassHolder each time.
 * 
 * @param <K>
 * @param <T>
 */
public class ChangeNotification<K, T extends DMObject<K>> implements Serializable {
	private static final long serialVersionUID = 3460039660076438219L;

	private static Logger logger = GlobalSetup.getLogger(ChangeNotification.class);

	private Class<T> clazz;
	private K key;
	private long propertyMask; // bitset
	private ClientMatcher matcher;
	
	private long[] feedTimestamps;

	/**
	 * DO NOT USE THIS CONSTRUCTOR DIRECTLY. Instead use model.makeChangeNotification(),
	 * which properly handles subclassing. 
	 */
	public ChangeNotification(Class<T> clazz, K key, ClientMatcher matcher) {
		this.clazz = clazz;
		this.key = key;
		this.matcher = matcher;
	}
	
	public void addProperty(int propertyIndex) {
		this.propertyMask |= 1 << propertyIndex;
	}
	
	public void addProperty(DataModel model, String propertyName) {
		@SuppressWarnings("unchecked")
		DMClassHolder<K,T> classHolder = (DMClassHolder<K,T>)model.getClassHolder(clazz);

		int propertyIndex = classHolder.getPropertyIndex(propertyName);
		if (propertyIndex < 0)
			throw new RuntimeException("Class " + classHolder.getDMOClass().getName() + " has no property " + propertyName);
		
		DMPropertyHolder<K,T,?> property = classHolder.getProperty(propertyIndex);
		if (property instanceof FeedPropertyHolder)
			throw new RuntimeException("For feed-valued-properties, you must use session.feedChanged()");
		
		addProperty(propertyIndex);
	}
	
	public void addFeedProperty(DataModel model, String propertyName, long itemTimestamp) {
		@SuppressWarnings("unchecked")
		DMClassHolder<K,T> classHolder = (DMClassHolder<K,T>)model.getClassHolder(clazz);

		int propertyIndex = classHolder.getPropertyIndex(propertyName);
		if (propertyIndex < 0)
			throw new RuntimeException("Class " + classHolder.getDMOClass().getName() + " has no property " + propertyName);
		
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
		@SuppressWarnings("unchecked")
		DMClassHolder<K,T> classHolder = (DMClassHolder<K,T>)model.getClassHolder(clazz);
		
		long v = propertyMask;
		int propertyIndex = 0;
		while (v != 0) {
			if ((v & 1) != 0) {
				DMPropertyHolder<K, T, ?> property = classHolder.getProperty(propertyIndex);
				
				if (property instanceof FeedPropertyHolder) {
					int feedPropertyIndex = classHolder.getFeedPropertyIndex(property.getName());
					model.getStore().invalidateFeed(classHolder, key, propertyIndex, timestamp, feedTimestamps[feedPropertyIndex]);
					
					logger.debug("Invalidated {}#{}.{}, feedTimestamp={}", new Object[] { 
							classHolder.getDMOClass().getSimpleName(),
							key, 
							property.getName(),
							feedTimestamps[feedPropertyIndex]});
				} else {
					model.getStore().invalidate(classHolder, key, propertyIndex, timestamp);
				
					logger.debug("Invalidated {}#{}.{}", new Object[] { 
							classHolder.getDMOClass().getSimpleName(),
							key, 
							property.getName() });
				}
			}
				
			propertyIndex++;
			v >>= 1;
		}
	}

	public void resolveNotifications(DataModel model, ClientNotificationSet result) {
		@SuppressWarnings("unchecked")
		DMClassHolder<K,T> classHolder = (DMClassHolder<K,T>)model.getClassHolder(clazz);

		model.getStore().resolveNotifications(classHolder, key, propertyMask, result, matcher);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ChangeNotification))
			return false;
		
		ChangeNotification<?,?> other = (ChangeNotification<?,?>)o;
		
		return clazz == other.clazz && key.equals(other.key);
	}
	
	@Override
	public int hashCode() {
		return 11 * clazz.hashCode() + 17 * key.hashCode();
	}
	
	@Override
	public String toString() {
		if (matcher != null)
			return clazz.getSimpleName() + "#" + key.toString() + "; matcher=" + matcher;
		else
			return clazz.getSimpleName() + "#" + key.toString();
	}
}
