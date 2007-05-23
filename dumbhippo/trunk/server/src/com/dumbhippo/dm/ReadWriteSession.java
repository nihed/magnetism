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
	

	@Override
	public <K, T extends DMObject<K>> Object fetchAndFilter(Class<T> clazz, K key, int propertyIndex) throws NotCachedException {
		throw new NotCachedException();
	}

	@Override
	public <K, T extends DMObject<K>> Object storeAndFilter(Class<T> clazz, K key, int propertyIndex, Object value) {
		return value;
	}

	private static class QueuedNotification<K,T extends DMObject<K>> {
		protected DMClassHolder<T> classHolder;
		protected K key;
		protected int propertyIndex;

		public QueuedNotification(DMClassHolder<T> classHolder, K key, int propertyIndex) {
			this.classHolder = classHolder;
			this.key = key;
			this.propertyIndex = propertyIndex;
		}
	}
	
	public <K, T extends DMObject<K>> void notify(Class<T> clazz, K key, String propertyName) {
		DMClassHolder<T> classHolder = model.getDMClass(clazz);
		int propertyIndex = classHolder.getPropertyIndex(propertyName);
		if (propertyIndex < 0)
			throw new RuntimeException("Class " + clazz.getName() + " has no property " + propertyName);

		notifications.add(new QueuedNotification<K,T>(classHolder, key, propertyIndex));
	}
	
	@Override
	public void afterCompletion(int status) {
		long timestamp = model.getTimestamp();
		
		for (QueuedNotification<?, ?> notification : notifications) {
			DMClassHolder<?> classHolder = notification.classHolder;
			model.getStore().invalidate(classHolder, notification.key, notification.propertyIndex, timestamp);
		
			logger.debug("Invalidated {}#{}.{}", new Object[] { 
					classHolder.getClass().getSimpleName(),
					notification.key, 
					classHolder.getProperty(notification.propertyIndex).getName() });
		}
	}
}
