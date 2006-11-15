package com.dumbhippo.server;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.FlickrPhotosetStatus;
import com.dumbhippo.persistence.FlickrUpdateStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.ExternalAccountsListener;
import com.dumbhippo.services.FlickrPhotoView;
import com.dumbhippo.services.FlickrPhotosetView;

/** 
 * This bean maintains the FlickrUpdateStatus table 
 * in the database.
 * 
 * @author Havoc Pennington
 *
 */
@Local
public interface FlickrUpdater extends ExternalAccountsListener {

	public FlickrUpdateStatus getCachedStatus(User user) throws NotFoundException;
	public FlickrUpdateStatus getCachedStatus(ExternalAccount flickrAccount) throws NotFoundException;
	public FlickrUpdateStatus getCachedStatus(String flickrId) throws NotFoundException;
	public Set<String> getActiveFlickrUserIds();
	public Collection<User> getUsersWhoLoveFlickrAccount(String flickrUserId);
	public Collection<FlickrPhotosetStatus> getPhotosetStatusesForFlickrAccount(String flickrUserId);
	// called by the "cron job"
	public void periodicUpdate(String flickrId);
	// called by periodicUpdate if it thinks there are new changes
	public void saveUpdatedStatus(String flickrId, int totalPhotos, int totalPhotosets,
			List<FlickrPhotoView> photoViews, List<FlickrPhotosetView> photosetViews);
}
