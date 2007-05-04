package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.AmazonListsView;
import com.dumbhippo.services.AmazonListView;

@Local
public interface AmazonListsCache extends Cache<String,AmazonListsView> {
	public AmazonListView queryExisting(String key, String listId);
}