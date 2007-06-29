package com.dumbhippo.dm.store;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.fetch.Fetch;

public class Registration<K, T extends DMObject<K>> {
	private StoreClient client;
	private StoreNode<K,T> node;
	private Fetch<K,? super T> fetch;
	
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

	public synchronized Fetch<K,? super T> addFetch(Fetch<K,? super T> newFetch) {
		Fetch<K,? super T> oldFetch = fetch;
		
		if (oldFetch != null) {
			Fetch<K, ? super T> mergedFetch = oldFetch.merge(newFetch);
			fetch = mergedFetch;
		} else
			fetch = newFetch;
		
		return oldFetch;
	}
	
	public void removeFromNode() {
		node.removeRegistration(this);
	}

	public synchronized Fetch<K,? super T> getFetch() {
		return fetch;
	}
}
