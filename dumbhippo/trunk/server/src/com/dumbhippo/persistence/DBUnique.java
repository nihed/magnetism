package com.dumbhippo.persistence;

public abstract class DBUnique {
	protected long id;
	
	protected DBUnique() {
	}
	
	public long getId() {
		return this.id;
	}
	
	protected void setId(long id) {
		this.id = id;
	}
}
