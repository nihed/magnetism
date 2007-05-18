package com.dumbhippo.dm;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

public abstract class DMObject<KeyType> {
	private KeyType key;
	Guid guid;
	
	protected DMObject(KeyType key) {
		this.key = key;
	}
	
	public KeyType getKey() {
		return key;
	}
	
	protected abstract void init() throws NotFoundException;
}
