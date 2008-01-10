package com.dumbhippo.server;

import java.util.Collection;
import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.Pair;
import com.dumbhippo.persistence.AmazonActivityStatus;
import com.dumbhippo.persistence.AmazonUpdateStatus;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;
import com.dumbhippo.services.AmazonItem;
import com.dumbhippo.services.AmazonListsView;
import com.dumbhippo.services.AmazonReviewsView;

/** 
 * This bean maintains the AmazonActivityStatus table 
 * in the database.
 * 
 * @author marinaz
 *
 */
@Local
public interface AmazonUpdater extends CachedExternalUpdater<AmazonUpdateStatus>, PollingTaskLoader {
	
	public Collection<AmazonActivityStatus> getActivityStatusesForAmazonAccount(String amazonUserId);
	
	public List<AmazonActivityStatus> saveUpdatedStatus(String amazonUserId, AmazonReviewsView reviewsView, AmazonListsView listsView);
	
	public void saveItemsInCache(List<AmazonItem> items);
	
	public List<Pair<String, String>> getAmazonLinks(String amazonUserId, boolean expireCache);
}
