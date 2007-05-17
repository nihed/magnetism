package com.dumbhippo.dm;

public interface DMSessionMap {
	public void initCurrent(DMSession session);
	public DMSession getCurrent();
}
