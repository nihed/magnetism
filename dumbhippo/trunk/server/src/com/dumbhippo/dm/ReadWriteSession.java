package com.dumbhippo.dm;


public class ReadWriteSession extends DMSession {
	protected ReadWriteSession(DMCache cache, DMViewpoint viewpoint) {
		super(cache, viewpoint);
	}
	
	public static ReadWriteSession getCurrent() {
		DMSession session = DMCache.getInstance().getCurrentSession();
		if (session instanceof ReadWriteSession)
			return (ReadWriteSession)session;
		
		throw new IllegalStateException("ReadWriteSession.getCurrent() called when not inside a ReadWriteSession");
	}
}
