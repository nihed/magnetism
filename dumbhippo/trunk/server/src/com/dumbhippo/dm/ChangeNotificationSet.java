package com.dumbhippo.dm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

	private Map<ChangeNotification<?,?>, ChangeNotification<?,?>> notifications;
	private List<ChangeNotification<?,?>> matchedNotifications;
	private long timestamp;
	
	public ChangeNotificationSet(DataModel model) {
	}

	public <K, T extends DMObject<K>> void changed(DataModel model, Class<T> clazz, K key, String propertyName, ClientMatcher matcher) {
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

		if (matcher != null) {
			if (matchedNotifications == null)
				matchedNotifications = new ArrayList<ChangeNotification<?,?>>();
			
			ChangeNotification<?,?> notification = new ChangeNotification<K,T>(clazz, key, matcher);
			notification.addProperty(propertyIndex);;
			matchedNotifications.add(notification);
		} else {
			if (notifications == null)
				notifications = new HashMap<ChangeNotification<?,?>, ChangeNotification<?,?>>();
	
			ChangeNotification<?,?> notification = new ChangeNotification<K,T>(clazz, key);
			ChangeNotification<?,?> oldNotification = notifications.get(notification);
			if (oldNotification != null) {
				oldNotification.addProperty(propertyIndex);
			} else {
				notification = new ChangeNotification<K,T>(clazz, key);
				notifications.put(notification, notification);
				notification.addProperty(propertyIndex);
			}
		}
	}
	

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public void doInvalidations(DataModel model) {
		if (notifications != null) {
			for (ChangeNotification<?, ?> notification : notifications.values())
				notification.invalidate(model, timestamp);
		}
		
		if (matchedNotifications != null) {
			// I can't think of any valid reason for doing a ClientMatch notification
			// on a cached property, so this is probably pointless.
			
			for (ChangeNotification<?, ?> notification : matchedNotifications)
				notification.invalidate(model, timestamp);
		}
	}
	
	public ClientNotificationSet resolveNotifications(DataModel model) {
		ClientNotificationSet clientNotifications = new ClientNotificationSet();
		
		if (notifications != null) {
			for (ChangeNotification<?, ?> changeNotification : notifications.values())
				changeNotification.resolveNotifications(model, clientNotifications);
		}
		if (matchedNotifications != null) {
			for (ChangeNotification<?, ?> changeNotification : matchedNotifications)
				changeNotification.resolveNotifications(model, clientNotifications);
		}
		
		return clientNotifications;
	}
	
	public boolean isEmpty() {
		return notifications == null && matchedNotifications == null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (notifications != null)
			sb.append(notifications.values().toString());
		
		if (notifications != null && matchedNotifications != null)
			sb.append(", ");
			
		if (matchedNotifications != null)
			sb.append(matchedNotifications.toString());
		
		return sb.toString();
	}
}
 