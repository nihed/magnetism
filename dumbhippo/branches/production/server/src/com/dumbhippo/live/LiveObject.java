package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

public abstract class LiveObject implements Ageable, Cloneable {
	private int cacheAge;
	private Guid guid;
	
	protected LiveObject(Guid guid) {
		this.guid = guid;
	}
	
	public int getCacheAge() {
		return cacheAge;
	}

	public Guid getGuid() {
		return guid;
	}

	public void setCacheAge(int cacheAge) {
		this.cacheAge = cacheAge;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LiveObject))
			return false;
		LiveObject obj = (LiveObject) o;
		return obj.guid.equals(guid);
	}

	@Override
	public final int hashCode() {
		return guid.hashCode();
	}
}
