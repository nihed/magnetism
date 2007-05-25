package com.dumbhippo.dm;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.store.StoreKey;

public class ChangeNotificationSet implements Runnable {
	@SuppressWarnings("unused")
	private static Logger logger = GlobalSetup.getLogger(ChangeNotificationSet.class);

	private Map<StoreKey, ChangeNotification> notifications = new HashMap<StoreKey, ChangeNotification>();
	private DataModel model;
	
	public ChangeNotificationSet(DataModel model) {
		this.model = model;
	}

	public <K, T extends DMObject<K>> void changed(Class<T> clazz, K key, String propertyName) {
		@SuppressWarnings("unchecked")
		DMClassHolder<K,T> classHolder = (DMClassHolder<K,T>)model.getClassHolder(clazz);
		int propertyIndex = classHolder.getPropertyIndex(propertyName);
		if (propertyIndex < 0)
			throw new RuntimeException("Class " + clazz.getName() + " has no property " + propertyName);

		ChangeNotification notification = new ChangeNotification<K,T>(model, classHolder, key);
		ChangeNotification oldNotification = notifications.get(notification);
		if (oldNotification != null) {
			oldNotification.addProperty(propertyIndex);
		} else {
			notification = new ChangeNotification<K,T>(model, classHolder, key);
			notifications.put(notification, notification);
			notification.addProperty(propertyIndex);
		}
	}
	
	public void commit() {
		long timestamp = model.getTimestamp();
		
		for (ChangeNotification<?, ?> notification : notifications.values())
			notification.invalidate(timestamp);

		model.notifyAsync(this);
	}

	public void run() {
		logger.debug("Sending notifications for {}", this);
		ClientNotificationSet clientNotifications = new ClientNotificationSet();
		for (ChangeNotification<?, ?> changeNotification : notifications.values())
			changeNotification.resolveNotifications(model.getStore(), clientNotifications);
		
		logger.debug("Resolved notifications are {}", clientNotifications);
		for (ClientNotification clientNotification : clientNotifications.getNotifications()) {
			model.sendNotification(clientNotification);
		}
	}
	
	@Override
	public String toString() {
		return notifications.values().toString();
	}
}
 