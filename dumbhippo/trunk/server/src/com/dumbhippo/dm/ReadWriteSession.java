package com.dumbhippo.dm;

import javax.transaction.Status;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.schema.FeedPropertyHolder;
import com.dumbhippo.dm.store.StoreKey;

/**
 * ReadWriteSession is a session used when modifying the data that is exposed as
 * DMObjects. It is possible to load DMOs in a ReadWriteSession, but the typical
 * use of a ReadWriteSession is the notifying property changes via it's
 * {@link #changed(Class, Object, String)} method.
 * 
 * @author otaylor
 */
public class ReadWriteSession extends CachedSession {
	@SuppressWarnings("unused")
	private static Logger logger = GlobalSetup.getLogger(ReadWriteSession.class);
	
	private ChangeNotificationSet notificationSet;

	protected ReadWriteSession(DataModel model, DMClient client, DMViewpoint viewpoint) {
		super(model, client, viewpoint);
		
		notificationSet = new ChangeNotificationSet(model);
	}
	
	@Override
	public <K, T extends DMObject<K>> Object fetchAndFilter(StoreKey<K,T> key, int propertyIndex) throws NotCachedException {
		throw new NotCachedException();
	}

	@Override
	public <K, T extends DMObject<K>> Object storeAndFilter(StoreKey<K,T> key, int propertyIndex, Object value) {
		DMPropertyHolder<K,T,?> property = key.getClassHolder().getProperty(propertyIndex);
		
		if (value == null)
			return null;
		else
			return property.filter(getViewpoint(), key.getKey(), value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <K, T extends DMObject<K>> DMFeed<?> createFeedWrapper(StoreKey<K, T> key, int propertyIndex, DMFeed<T> rawFeed) {
		FeedPropertyHolder<K, T, ?, ?> feedProperty = (FeedPropertyHolder<K, T, ?, ?>)key.getClassHolder().getProperty(propertyIndex); 
		return new FeedWrapper(feedProperty, key.getKey(), rawFeed, null);
	}

	/**
	 * Indicate that a resource property has changed; this invalidates any cached value for the
	 * property and also triggers sending notifications to any clients that have registered
	 * for notification on the property. Notifications will only be sent after the current
	 * transaction commits succesfully. For changes to feed-valued properties, see feedChanged().
	 * 
	 * @param <K>
	 * @param <T>
	 * @param clazz the class of the resource where the property changed
	 * @param key the key of the resource  where the property changed
	 * @param propertyName the name of the property that changed
	 */
	public <K, T extends DMObject<K>> void changed(Class<T> clazz, K key, String propertyName) {
		changed(clazz, key, propertyName, null);
	}
	
	/**
	 * Indicate that a feed-valued resource property has changed; This differs from changed()
	 * in that the timestamp of the provided item is also provided.
	 * 
	 * @param <K>
	 * @param <T>
	 * @param clazz the class of the resource where the property changed
	 * @param key the key of the resource  where the property changed
	 * @param propertyName the name of the property that changed
	 * @param itemTimestamp new timestamp of the item that was inserted or restacked, or -1 to indicate
	 *    that an item was deleted.
	 */
	public <K, T extends DMObject<K>> void feedChanged(Class<T> clazz, K key, String propertyName, long itemTimestamp) {
		notificationSet.feedChanged(model, clazz, key, propertyName, itemTimestamp);
	}
	
	/**
	 * Indicate that a resource property has changed; this invalidates any cached value for the
	 * property and also triggers sending notifications to any clients that have registered
	 * for notification on the property. Notifications will only be sent after the current
	 * transaction commits succesfully.
	 * 
	 * @param <K>
	 * @param <T>
	 * @param clazz the class of the resource where the property changed
	 * @param key the key of the resource  where the property changed
	 * @param propertyName the name of the property that changed
	 * @param matcher used to determine what clients to notify of the change; this would
	 *   typically be used for a change to a viewer-dependent uncached property. 
	 */
	public <K, T extends DMObject<K>> void changed(Class<T> clazz, K key, String propertyName, ClientMatcher matcher) {
		notificationSet.changed(model, clazz, key, propertyName, matcher);
		
		// FIXME: invalidate the property in the session-local cached object if one exists
	}
	
	/**
	 * Indicates that a resource has been deleted. This currently doesn't send a notification
	 * to clients, it just cleans up internal data structures that cache information about
	 * that object, so that future requests for that information will not fetch the
	 * cached information. (It is in fact hooked up to infrastructure for notifying of
	 * an 'eviction' from the data model, but we don't send those out over XMPP, and
	 * even if we did, there would be no indication that the resource is actually gone,
	 * and not just no longer cache.d)
	 * 
	 * @param clazz the class of the resource that was deleted
	 * @param key the key of the resource that was deleted
	 */
	public <K, T extends DMObject<K>> void removed(Class<T> clazz, K key) {
		notificationSet.removed(model, clazz, key);
	}

	@Override
	public void afterCompletion(int status) {
		if (status == Status.STATUS_COMMITTED)
			model.commitChanges(notificationSet);
	}
}
