package com.dumbhippo.server;

import java.util.Collection;

import javax.ejb.Local;

import com.dumbhippo.persistence.AmazonActivityStatus;
import com.dumbhippo.persistence.AmazonUpdateStatus;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;
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
	
	public boolean saveUpdatedStatus(String amazonUserId, AmazonReviewsView reviewsView, AmazonListsView listsView);
}
