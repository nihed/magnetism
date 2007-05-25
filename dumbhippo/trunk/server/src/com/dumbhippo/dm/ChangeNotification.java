package com.dumbhippo.dm;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.store.DMStore;
import com.dumbhippo.dm.store.StoreKey;

public class ChangeNotification<K, T extends DMObject<K>> extends StoreKey<K,T> {
	private static Logger logger = GlobalSetup.getLogger(ChangeNotification.class);

	private DataModel model;
	private long propertyMask; // bitset

	public ChangeNotification(DataModel model, DMClassHolder<K,T> classHolder, K key) {
		super(classHolder, key);
		this.model = model;
	}
	
	public void addProperty(int propertyIndex) {
		this.propertyMask |= 1 << propertyIndex;
	}
	
	public long getPropertyMask() {
		return propertyMask;
	}
	
	public void invalidate(long timestamp) {
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

	public void resolveNotifications(DMStore store, ClientNotificationSet result) {
		store.resolveNotifications(this, result);
	}
	
	@Override
	public String toString() {
		return "{" + super.toString() + ", propertyMask=" + propertyMask + "}"; 
	}
}
