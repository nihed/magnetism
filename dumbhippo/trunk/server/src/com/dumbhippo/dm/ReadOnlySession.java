package com.dumbhippo.dm;


public class ReadOnlySession extends DMSession {
	protected ReadOnlySession(DMCache cache, DMViewpoint viewpoint) {
		super(cache, viewpoint);
	}
	
	public static ReadOnlySession getCurrent() {
		DMSession session = DMCache.getInstance().getCurrentSession();
		if (session instanceof ReadOnlySession)
			return (ReadOnlySession)session;
		
		throw new IllegalStateException("ReadOnlySession.getCurrent() called when not inside a ReadOnlySession");
	}
}
