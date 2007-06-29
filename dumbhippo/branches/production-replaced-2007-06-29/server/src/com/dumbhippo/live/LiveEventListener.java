package com.dumbhippo.live;

public interface LiveEventListener<T extends LiveEvent> {
	public void onEvent(T event);
}
