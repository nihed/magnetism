package com.dumbhippo.dm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

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
	private boolean autoNotify = true;
	
	public ChangeNotificationSet(DataModel model) {
	}

	private <K, T extends DMObject<K>> ChangeNotification<K,T> getNotification(DataModel model, Class<T> clazz, K key, ClientMatcher matcher) {
		if (key instanceof DMKey) {
			@SuppressWarnings("unchecked")
			K clonedKey = (K)((DMKey)key).clone(); 
			key = clonedKey;
		}

		if (matcher != null) {
			if (matchedNotifications == null)
				matchedNotifications = new ArrayList<ChangeNotification<?,?>>();
			
			ChangeNotification<K,T> notification = model.makeChangeNotification(clazz, key, null);
			matchedNotifications.add(notification);
			return notification;
		} else {
			if (notifications == null)
				notifications = new HashMap<ChangeNotification<?,?>, ChangeNotification<?,?>>();
	
			ChangeNotification<K,T> notification = model.makeChangeNotification(clazz, key, matcher);
			@SuppressWarnings("unchecked")
			ChangeNotification<K,T> oldNotification = (ChangeNotification<K,T>)notifications.get(notification);
			if (oldNotification != null) {
				return oldNotification;
			} else {
				notifications.put(notification, notification);
				return notification;
			}
		}
		
	}

	public <K, T extends DMObject<K>> void changed(DataModel model, Class<T> clazz, K key, String propertyName, ClientMatcher matcher) {
		ChangeNotification<K,T> notification = getNotification(model, clazz, key, matcher);
		notification.addProperty(model, propertyName);
	}

	public <K, T extends DMObject<K>> void feedChanged(DataModel model, Class<T> clazz, K key, String propertyName, long itemTimestamp) {
		ChangeNotification<K,T> notification = getNotification(model, clazz, key, null);
		notification.addFeedProperty(model, propertyName, itemTimestamp);
	}

	public <K, T extends DMObject<K>> void deleted(DataModel model, Class<T> clazz, K key) {
		ChangeNotification<K,T> notification = getNotification(model, clazz, key, null);
		notification.deleted();
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

	public boolean getAutoNotify() {
		return autoNotify;
	}

	/**
	 * Normally notifications are sent out to local clients asynchronously
	 * at some point after the transaction commits. If you set autoNotify
	 * to false, then this doesn't happen and you must call DataModel.sendNotifications()
	 * manually to trigger the local notifications. (invalidations and notifications 
	 * on other cluster nodes will happen as per normal.) This is useful in the
	 * case where you want to control the order that notifications are sent out
	 * with respect to some other event. 
	 * 
	 * @param autoNotify whether to automatically send out local notifications
	 */
	public void setAutoNotify(boolean autoNotify) {
		this.autoNotify = autoNotify;
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
 