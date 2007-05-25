package com.dumbhippo.dm;

import javax.transaction.Status;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.store.StoreKey;


public class ReadWriteSession extends CachedSession {
	@SuppressWarnings("unused")
	private static Logger logger = GlobalSetup.getLogger(ReadWriteSession.class);
	
	private ChangeNotificationSet notificationSet;

	protected ReadWriteSession(DataModel model, DMViewpoint viewpoint) {
		super(model, viewpoint);
		
		notificationSet = new ChangeNotificationSet(model);
	}
	
	public static ReadWriteSession getCurrent() {
		DMSession session = DataModel.getInstance().getCurrentSession();
		if (session instanceof ReadWriteSession)
			return (ReadWriteSession)session;
		
		throw new IllegalStateException("ReadWriteSession.getCurrent() called when not inside a ReadWriteSession");
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

	public <K, T extends DMObject<K>> void changed(Class<T> clazz, K key, String propertyName) {
		notificationSet.changed(clazz, key, propertyName);
	}
	
	@Override
	public void afterCompletion(int status) {
		if (status == Status.STATUS_COMMITTED)
			notificationSet.commit();
	}
}
