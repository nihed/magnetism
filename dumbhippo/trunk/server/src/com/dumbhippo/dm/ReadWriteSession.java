package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;


public class ReadWriteSession extends DMSession {
	private static Logger logger = GlobalSetup.getLogger(ReadWriteSession.class);
	private List<QueuedNotification> notifications = new ArrayList<QueuedNotification>();

	protected ReadWriteSession(DataModel model, DMViewpoint viewpoint) {
		super(model, viewpoint);
	}
	
	public static ReadWriteSession getCurrent() {
		DMSession session = DataModel.getInstance().getCurrentSession();
		if (session instanceof ReadWriteSession)
			return (ReadWriteSession)session;
		
		throw new IllegalStateException("ReadWriteSession.getCurrent() called when not inside a ReadWriteSession");
	}
	

	public <K, T extends DMObject<K>> Object fetchAndFilter(Class<T> clazz, K key, int propertyIndex) throws NotCachedException {
		throw new NotCachedException();
	}

	public <K, T extends DMObject<K>> Object storeAndFilter(Class<T> clazz, K key, int propertyIndex, Object value) {
		return value;
	}

	private static class QueuedNotification<K,T extends DMObject<K>> {
		protected Class<T> clazz;
		protected K key;
		protected String propertyName;

		public QueuedNotification(Class<T> clazz, K key, String propertyName) {
			this.clazz = clazz;
			this.key = key;
			this.propertyName = propertyName;
		}
	}
	
	public <K, T extends DMObject<K>> void notify(Class<T> clazz, K key, String propertyName) {
		notifications.add(new QueuedNotification<K,T>(clazz, key, propertyName));
	}
	
	public void afterCompletion(int status) {
		long timestamp = model.getTimestamp();
		
		for (QueuedNotification<?, ?> notification : notifications) {
			DMClassHolder classHolder = model.getDMClass(notification.clazz);
			int propertyIndex = classHolder.getPropertyIndex(notification.propertyName); 
			model.getStore().invalidate(classHolder, notification.key, propertyIndex, timestamp);
		
			logger.debug("Invalidated {}#{}.{}", new Object[] { notification.clazz.getSimpleName(), notification.key, notification.propertyName });
		}
	}
}
