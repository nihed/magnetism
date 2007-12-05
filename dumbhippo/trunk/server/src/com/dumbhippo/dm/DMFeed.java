package com.dumbhippo.dm;

import java.util.Iterator;

/**
 * This class represents a feed of items; each feed item has a
 * resource value and a timestamp.
 * 
 * @param <T> the value type; this class is used for both lists of feed items
 *   as seen by applications and for "dehydrated" feed item list with
 *   just the keys for the resources, which is why it isn't 'T extends DMObject'.  
 */
public interface DMFeed<T> {
	/**
	 * Iterate the items in the feed
	 * 
	 * @param start start position for iteration (0 is the most recent item in the feed)
	 * @param max maximum number of items to return
	 * @param minTimestamp only iterate items with this timestamp or newer 
	 */
	public Iterator<DMFeedItem<T>> iterator(int start, int max, long minTimestamp);
}
