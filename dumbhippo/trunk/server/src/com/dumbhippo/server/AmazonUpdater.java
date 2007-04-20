package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.AmazonUpdateStatus;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;
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
	
	public boolean saveUpdatedStatus(String amazonUserId, AmazonReviewsView reviewsView);
	
}
