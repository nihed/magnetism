package com.dumbhippo.live;

public abstract class LiveObject implements Ageable, Cloneable {
	private int cacheAge;
	
	public int getCacheAge() {
		return cacheAge;
	}

	public void setCacheAge(int cacheAge) {
		this.cacheAge = cacheAge;
	}
}
