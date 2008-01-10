package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.AmazonReviewView;
import com.dumbhippo.services.AmazonReviewsView;

@Local
public interface AmazonReviewsCache extends Cache<String,AmazonReviewsView> {
	public AmazonReviewView queryExisting(String key, String itemId);
}