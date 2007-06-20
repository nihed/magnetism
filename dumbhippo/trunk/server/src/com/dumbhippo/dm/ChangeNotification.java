package com.dumbhippo.dm;

import java.io.Serializable;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.schema.DMClassHolder;

/**
 * ChangeNotification represents pending notifications for a single resource.
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

	public ChangeNotification(Class<T> clazz, K key) {
		this.clazz = clazz;
		this.key = key;
	}
	
	public void addProperty(int propertyIndex) {
		this.propertyMask |= 1 << propertyIndex;
	}
	
	public long getPropertyMask() {
		return propertyMask;
	}
	
	public void invalidate(DataModel model, long timestamp) {
		@SuppressWarnings("unchecked")
		DMClassHolder<K,T> classHolder = (DMClassHolder<K,T>)model.getClassHolder(clazz);
		
		long v = propertyMask;
		int propertyIndex = 0;
		while (v != 0) {
			if ((v & 1) != 0) {
				model.getStore().invalidate(classHolder, key, propertyIndex, timestamp);
				
				logger.debug("Invalidated {}#{}.{}", new Object[] { 
						classHolder.getClass().getSimpleName(),
						key, 
						classHolder.getProperty(propertyIndex).getName() });
			}
				
			propertyIndex++;
			v >>= 1;
		}
	}

	public void resolveNotifications(DataModel model, ClientNotificationSet result) {
		@SuppressWarnings("unchecked")
		DMClassHolder<K,T> classHolder = (DMClassHolder<K,T>)model.getClassHolder(clazz);

		model.getStore().resolveNotifications(classHolder, key, propertyMask, result);
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
		return clazz.getSimpleName() + "#" + key.toString();
	}
}
