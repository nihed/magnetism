package com.dumbhippo.services.caches;

import javax.ejb.Local;
import com.dumbhippo.services.AmazonReviewsView;

@Local
public interface AmazonReviewsCache extends Cache<String,AmazonReviewsView> {

}