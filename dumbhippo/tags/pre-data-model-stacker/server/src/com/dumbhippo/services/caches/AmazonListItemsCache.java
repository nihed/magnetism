package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.Pair;
import com.dumbhippo.services.AmazonListItemView;
import com.dumbhippo.services.AmazonListView;

// The key to AmazonListItemsCache is a Pair of two Strings, amazonUserId and listId.
// There are two reasons for this. First, this allows us to use CachedAmazonList as
// both a list item in a a list of someone's Amazon lists (keyed of an amazonUserId)
// and a summary storage for a list (keyed of a listId). Second, theoretically the
// same list could belong to multiple Amazon users if it's a WeddingRegistry, though
// I don't know how Amazon is handling that. 
@Local
public interface AmazonListItemsCache extends Cache<Pair<String, String>,AmazonListView> {
	public AmazonListItemView queryExisting(Pair<String, String> key, String itemId);
}