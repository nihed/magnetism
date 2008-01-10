package com.dumbhippo.dm;

import java.util.HashMap;
import java.util.Map;

/**
 *  This class acts as a store for information about a single feed inside the
 * data model cache.
 * 
 * The way that concurrency works is that as long as two different threads
 * have the same view of what items are in the feed, and as long as items are
 * always added from the most recent item back without gaps, then both threads
 * can fill items into the cache at the same time and it will work fine ... duplicates
 * will be eliminated and at any point, we'll see the cache having the first
 * max(M,N) items filled ... where M is the number filled by thread 1, and N
 * is the number filled by thread 2.
 *  
 * (The code here does support adding items out of order, and then sorts the items
 * into order, but the concurrency will no longer work right, since a reader will
 * think that the N items in the cache at any point of time are the *first*
 * N items.)
 */
public class CachedFeed<K> {
	private DMFeedItem items[] = new DMFeedItem[10];
	private Map<K, Integer> itemMap = new HashMap<K, Integer>(); 
	int itemCount = 0;
	int maxFetched = 0;
	
	private DMFeedItem<K> removeItem(int pos) {
		@SuppressWarnings("unchecked")
		DMFeedItem<K> item  = items[pos];
		
		System.arraycopy(items, pos + 1, items, pos, itemCount - (pos + 1));
		itemCount--;
		
		return item;
	}
	
	/**
	 * Add an item into the cache; the item is inserted at a position
	 * corresponding to its timestmap. If the items is already in the cache 
	 * it will be restacked with the new timestamp. (Restacking and out
	 * of order insertion are not used currently, since we always start from
	 * a an empty cache and fill it in order. If we did support mutating
	 * the cache as the feed changed, then considerably more work would
	 * have to be put into concurrency issues, possibly including locking
	 * the cache across the entire cluster.)
	 */
	public synchronized void addItem(K value, long timestamp) {
		Integer oldPos = itemMap.get(value);
		DMFeedItem<K> item;
		
		if (oldPos != null) {
			item = removeItem(oldPos);
			item.time = timestamp;
		} else {
			item = new DMFeedItem<K>(value, timestamp);
		}
		
		int pos = itemCount;
		while (pos > 0 && timestamp >= items[pos -1].time)
			pos--;
		
		if (itemCount >= items.length) {
			int newLength = items.length * 2;
			if (newLength < 0)
				throw new OutOfMemoryError();
			DMFeedItem newItems[] = new DMFeedItem[newLength];
			System.arraycopy(items, 0, newItems, 0, itemCount);
			items = newItems;
		}
		
		if (pos < itemCount)
			System.arraycopy(items, pos, items, pos + 1, itemCount - pos);
		
		items[pos] = new DMFeedItem<K>(value, timestamp);
		itemCount++;
		
		itemMap.put(value, pos);
	}

	/**
	 * Get an item from the cached feed store.
	 * 
	 * @param pos position at which to get the item. 0 is the most recent
	 *   item in the feed.
	 */
	@SuppressWarnings("unchecked")
	public synchronized DMFeedItem<K> getItem(int pos) {
		return items[pos];
	}
	
	/**
	 * Get the total number of items in the store.
	 */
	public synchronized int size() {
		return itemCount;
	}
	
	/**
	 * Get the number of items that would have been stored in the cached
	 * feed store if they were present in feed.
	 * 
	 * We need to track this separately from the actual number of stored
	 * items so that we don't repeatedly query the database for items that
	 * aren't actually there.   
	 */
	public synchronized int getMaxFetched() {
		return maxFetched;
	}
	
	/**
	 * Increase the value returned by getMaxFetched(). A thread fetching
	 * items from the feed and caching them in the cached feed store would
	 * call this, after storing all the items it fetched, with the number
	 * of items from the feed that would have been stored if the feed
	 * had all the items it asked for. 
	 * 
	 * @newMaxFetched the number of items that were attempted to be fetched
	 *  (this includes previously fetched items, so if the thread fetched
	 *  with start=15 and max=10, it would pass 25 for this parameter)
	 */
	public synchronized void addToMaxfetched(int newMaxFetched) {
		if (newMaxFetched > maxFetched)
			maxFetched = newMaxFetched;
	}
}
