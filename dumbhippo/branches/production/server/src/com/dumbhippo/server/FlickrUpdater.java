package com.dumbhippo.server;

import java.util.Collection;

import javax.ejb.Local;

import com.dumbhippo.persistence.FlickrPhotosetStatus;
import com.dumbhippo.persistence.FlickrUpdateStatus;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;
import com.dumbhippo.services.FlickrPhotosView;
import com.dumbhippo.services.FlickrPhotosetsView;

/** 
 * This bean maintains the FlickrUpdateStatus table 
 * in the database.
 * 
 * @author Havoc Pennington
 *
 */
@Local
public interface FlickrUpdater extends CachedExternalUpdater<FlickrUpdateStatus>, PollingTaskLoader {
	public Collection<FlickrPhotosetStatus> getPhotosetStatusesForFlickrAccount(String flickrUserId);

	// called by periodicUpdate if it thinks there are new changes
	public boolean saveUpdatedStatus(String flickrId, FlickrPhotosView photosView, FlickrPhotosetsView photosetsView);
}
