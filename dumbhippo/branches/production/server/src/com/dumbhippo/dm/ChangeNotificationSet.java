package com.dumbhippo.dm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.schema.DMClassHolder;

/**
 * A ChangeNotificationSet stores information about all changes to the data model that
 * are notified through {@link DMSession#changed()} during a single read-write session.
 * After the transaction for the session is committed succesfully, a ChangeNotificationSet
 * is resolved into a {@link ClientNotificationSet}, and notifications are sent to the
 * clients. 
 *  
 * @author otaylor
 */
public class ChangeNotificationSet implements Serializable {
	private static final long serialVersionUID = 8736760824100549330L;

	@SuppressWarnings("unused")
	private static Logger logger = GlobalSetup.getLogger(ChangeNotificationSet.class);

	private Map<ChangeNotification, ChangeNotification> notifications = new HashMap<ChangeNotification, ChangeNotification>();
	private long timestamp;
	
	public ChangeNotificationSet(DataModel model) {
	}

	public <K, T extends DMObject<K>> void changed(DataModel model, Class<T> clazz, K key, String propertyName) {
		@SuppressWarnings("unchecked")
		DMClassHolder<K,T> classHolder = (DMClassHolder<K,T>)model.getClassHolder(clazz);
		int propertyIndex = classHolder.getPropertyIndex(propertyName);
		if (propertyIndex < 0)
			throw new RuntimeException("Class " + clazz.getName() + " has no property " + propertyName);
		
		if (key instanceof DMKey) {
			@SuppressWarnings("unchecked")
			K clonedKey = (K)((DMKey)key).clone(); 
			key = clonedKey;
		}

		ChangeNotification notification = new ChangeNotification<K,T>(clazz, key);
		ChangeNotification oldNotification = notifications.get(notification);
		if (oldNotification != null) {
			oldNotification.addProperty(propertyIndex);
		} else {
			notification = new ChangeNotification<K,T>(clazz, key);
			notifications.put(notification, notification);
			notification.addProperty(propertyIndex);
		}
	}
	

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public void doInvalidations(DataModel model) {
		for (ChangeNotification<?, ?> notification : notifications.values())
			notification.invalidate(model, timestamp);
	}
	
	public ClientNotificationSet resolveNotifications(DataModel model) {
		ClientNotificationSet clientNotifications = new ClientNotificationSet();
		for (ChangeNotification<?, ?> changeNotification : notifications.values())
			changeNotification.resolveNotifications(model, clientNotifications);
		
		return clientNotifications;
	}
	
	public boolean isEmpty() {
		return notifications.isEmpty();
	}

	@Override
	public String toString() {
		return notifications.values().toString();
	}
}
 