package com.dumbhippo.dm.store;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.fetch.BoundFetch;

public class Registration<K, T extends DMObject<K>> {
	private StoreClient client;
	private StoreNode<K,T> node;
	private BoundFetch<K,? super T> fetch;
	private long[] feedTxTimestamps;
	
	public Registration(StoreNode<K,T> node, StoreClient client) {
		this.node = node;
		this.client = client;
	}
	
	public StoreNode<K,T> getNode() {
		return node;
	}
	
	public StoreClient getClient() {
		return client;
	}

	public synchronized BoundFetch<K,? super T> addFetch(BoundFetch<K,? super T> newFetch) {
		BoundFetch<K,? super T> oldFetch = fetch;
		
		if (oldFetch != null) {
			@SuppressWarnings("unchecked")
			BoundFetch<K, ? super T> mergedFetch = (BoundFetch<K, ? super T>)oldFetch.merge(newFetch);
			fetch = mergedFetch;
		} else
			fetch = newFetch;
		
		return oldFetch;
	}
	
	public void removeFromNode() {
		node.removeRegistration(this);
	}

	public synchronized BoundFetch<K,? super T> getFetch() {
		return fetch;
	}

	public long updateFeedTimestamp(int feedPropertyIndex, long newTimestamp) {
		/* We can have the situation where:
		 * 
		 *   T1) Notification updates the timestamp and fetches new items
		 *   T2) Fetch finds no new items with the updated timestamp
		 *   T2) Fetch returns nothing
		 *   T1) Notification is sent out
		 *
		 */
		long oldTimestamp;
		
		// Synchronizing the entire method would risk a deadlock, because of the
		// call to the synchronized StoreNode.getFeedLock() below
		synchronized(this) {
			if (feedTxTimestamps == null) {
				feedTxTimestamps = new long[node.getClassHolder().getFeedPropertiesCount()];
				for (int i = 0; i < feedTxTimestamps.length; i++)
					feedTxTimestamps[i] = -1;
			}
			
			oldTimestamp = feedTxTimestamps[feedPropertyIndex];
			feedTxTimestamps[feedPropertyIndex] = newTimestamp;
		}
		
		long result;
		if (oldTimestamp == -1) {
			// -1 is the "haven't previously updated the timestamp" timestamp, so we use 0
			// to signal "everything"
			result = 0;
		} else {
			FeedLog log = node.getFeedLog(feedPropertyIndex);
			if (log != null)
				result = log.getMinItemTimestamp(oldTimestamp);
			else
				result = Long.MAX_VALUE;
		}
		
		return result;
	}
}
