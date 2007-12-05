package com.dumbhippo.dm;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.filter.CompiledItemFilter;
import com.dumbhippo.dm.schema.FeedPropertyHolder;

/**
 * This class takes the raw DMFeed item as returned from the DMO and adds caching and
 * filtering to it. For feeds, unlike other property types, this is a dynamic process.
 * As items are fetched from the feed, they are filtered and cached.
 * 
 * Note that the start/max positions passed to iterator() are positions in the
 * *unfiltered* stream. If the all max items are filtered out, no items will be
 * returned. While this is less than ideal, it keeps things much simpler and 
 * more efficient than the alternative, since we know ahead of time how many
 * items to fetch from the underlying feed to get max items, and we don't have to 
 * worry about how many items were filtered before 'start'. As long as the fraction
 * of filtered items is small, the user isn't going to notice if their page of 10
 * items only has 8 on it. 
 */
public class FeedWrapper<K, T extends DMObject<K>, KI, TI extends DMObject<KI>> implements DMFeed<TI> {
	@SuppressWarnings("unused")
	static final private Logger logger = GlobalSetup.getLogger(FeedWrapper.class);

	private K key;
	private FeedPropertyHolder<K, T, KI, TI> property;
	private CachedFeed<KI> cachedFeed;
	private DMFeed<TI> rawFeed;
	
	/**
	 * @param property the property holder object for this property
	 * @param key the key of the object that has the feed as a property
	 * @param rawFeed the DMFeed returned by the DMO; this will typically query the database when iterated.
	 * @param cachedFeed cached feed object; this can be null if the current transaction is stale
	 *    with respect to the invalidation timestamp in the cache. (Perhaps we should create a dummy CachedFeed 
	 *    in this case? there is considerable inefficiency the way we do it currently since we'll requery
	 *    the database each time we are iterated when cachedFeed is null. Still it's a rare case)
	 */
	public FeedWrapper(FeedPropertyHolder<K, T, KI,TI> property, K key, DMFeed<TI> rawFeed, CachedFeed<KI> cachedFeed) {
		this.key = key;
		this.property = property;
		this.rawFeed = rawFeed;
		this.cachedFeed = cachedFeed;
	}
	
	public Iterator<DMFeedItem<TI>> iterator(int start, int max, long minTimestamp) {
		return new FeedWrapperIterator(start, max, minTimestamp);
	}
	
	private class FeedWrapperIterator implements Iterator<DMFeedItem<TI>> {
		private int start;
		private int max;
		private long minTimestamp;
		
		// Because filtering will remove items that are there in the underlying
		// feed, we need to fetch one item ahead to give an accurate value for
		// hasNext().
		private boolean fetchedNext = false;
		private DMFeedItem<TI> nextItem = null;
		
		private int pos;
		
		private Iterator<DMFeedItem<TI>> sourceIterator;
		
		public FeedWrapperIterator(int start, int max, long minTimestamp) {
			this.start = start;
			this.max = max;
			this.minTimestamp = minTimestamp;
			
			pos = start;
		}
		
		private void fetchNext() {
			DMSession session = property.getModel().currentSession();
			CompiledItemFilter<K, T, KI, TI> itemFilter = property.getItemFilter();
			
			nextItem = null;
			fetchedNext = true;
			
			if (sourceIterator == null && cachedFeed != null) {
				/* We start off by fetching items from the cache
				 */ 
				while (pos < max && pos < cachedFeed.size()) {
					DMFeedItem<KI> cachedItem = cachedFeed.getItem(pos);
	
					KI filteredKey;
					if (itemFilter != null)
						filteredKey = itemFilter.filterKey(session.getViewpoint(), key, cachedItem.getValue());
					else
						filteredKey = cachedItem.getValue();
					
					if (filteredKey != null) {
						nextItem = new DMFeedItem<TI>(property.rehydrateDMO(filteredKey, session), cachedItem.getTime());
						pos++;
						return;
					}
					
					pos++;
				}
			}
			
			if (pos == max)
				return;

			/* Check if the cachedFeed was the result of a fetch that asked for
			 * as many or more items than the current fetch but only returned a limited
			 * amount of items; in that case, we don't need to query the database again.
			 */
			if (cachedFeed != null && cachedFeed.getMaxFetched() >= start + max)
				return;
			
			/*
			 * If there are more items left to fetch once the cache is exhausted, we iterate 
			 * from the raw source feed and cache those new items as we go along.  
			 */
			
			if (sourceIterator == null) {
				if (cachedFeed != null) {
					/* We don't want to leave gaps in what is stored in cachedFeed, so we
					 * may need to start source iterator before pos, and then skip over
					 * some number of items 
					 */
					int skipPos = cachedFeed.size();
					sourceIterator = rawFeed.iterator(skipPos, start + max - skipPos, minTimestamp);
					
					while (skipPos < pos && sourceIterator.hasNext()) {
						DMFeedItem<TI> item = sourceIterator.next();
						skipPos++;
						cachedFeed.addItem(item.getValue().getKey(), item.getTime());
					}
				} else {
					sourceIterator = rawFeed.iterator(pos, start + max - pos, minTimestamp);
				}
			}

			while (sourceIterator.hasNext()) {
				DMFeedItem<TI> item = sourceIterator.next();
				pos++;
			
				if (cachedFeed != null)
					cachedFeed.addItem(item.getValue().getKey(), item.getTime());
				
				TI filteredValue;
				if (itemFilter != null)
					filteredValue = itemFilter.filterObject(session.getViewpoint(), key, item.getValue());
				else
					filteredValue = item.getValue();
				
				if (filteredValue != null) {
					nextItem = item;
					return;
				}
			}
	
			if (cachedFeed != null)
				cachedFeed.addToMaxfetched(start + max);
		}
		
		public boolean hasNext() {
			if (!fetchedNext)
				fetchNext();
			
			return nextItem != null && nextItem.getTime() >= minTimestamp;
		}

		public DMFeedItem<TI> next() {
			if (!fetchedNext)
				fetchNext();
			
			DMFeedItem<TI> result = nextItem;
			if (result == null || result.getTime() < minTimestamp)
				throw new NoSuchElementException();
			
			nextItem = null;
			fetchedNext = false;
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
