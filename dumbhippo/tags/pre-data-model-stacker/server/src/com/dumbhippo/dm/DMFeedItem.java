package com.dumbhippo.dm;

import java.util.Date;

/**
 * This class represents a single item in a feed; each feed item has a
 * resource value and a timestamp.
 * 
 * @param <T> the value type; this class is used for both feed items
 *   as seen by applications and for "dehydrated" feed items with
 *   just the key for the resource, which is why it isn't 'T extends DMObject'.  
 */
public class DMFeedItem<T> {
	protected T value;
	protected long time;
	
	public DMFeedItem(T value, long time) {
		this.value = value;
		this.time = time;
	}
	
	public T getValue() {
		return value;
	}
	
	public long getTime() {
		return time;
	}
	
	public Date getDate() {
		return new Date(time);
	}
}
