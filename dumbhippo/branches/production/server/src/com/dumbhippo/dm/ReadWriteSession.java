package com.dumbhippo.dm;

import javax.transaction.Status;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.schema.DMPropertyHolder;
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
		
		return property.filter(getViewpoint(), key.getKey(), value);
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
	 */
	public <K, T extends DMObject<K>> void changed(Class<T> clazz, K key, String propertyName) {
		notificationSet.changed(model, clazz, key, propertyName);
		
		// FIXME: invalidate the property in the session-local cached object if one exists
	}
	
	@Override
	public void afterCompletion(int status) {
		if (status == Status.STATUS_COMMITTED)
			model.commitChanges(notificationSet);
	}
}
