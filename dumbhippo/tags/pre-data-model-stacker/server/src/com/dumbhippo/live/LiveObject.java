package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

public abstract class LiveObject implements Cloneable {
	private Guid guid;
	
	protected LiveObject(Guid guid) {
		this.guid = guid;
	}
	
	public Guid getGuid() {
		return guid;
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
