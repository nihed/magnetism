package com.dumbhippo.dm;


public class ReadWriteSession extends DMSession {
	protected ReadWriteSession(DataModel model, DMViewpoint viewpoint) {
		super(model, viewpoint);
	}
	
	public static ReadWriteSession getCurrent() {
		DMSession session = DataModel.getInstance().getCurrentSession();
		if (session instanceof ReadWriteSession)
			return (ReadWriteSession)session;
		
		throw new IllegalStateException("ReadWriteSession.getCurrent() called when not inside a ReadWriteSession");
	}
	
	public <K, T extends DMObject<K>> Object fetchAndFilter(Class<T> clazz, K key, String propertyName) throws NotCachedException {
		throw new NotCachedException();
	}

	public <K, T extends DMObject<K>> Object storeAndFilter(Class<T> clazz, K key, String propertyName, Object value) {
		return value;
	}
}
