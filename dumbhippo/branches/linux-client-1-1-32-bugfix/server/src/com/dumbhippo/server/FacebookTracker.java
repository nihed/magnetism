package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.CachedFacebookPhotoData;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.server.views.UserViewpoint;

@Local
public interface FacebookTracker {

	public void updateOrCreateFacebookAccount(UserViewpoint viewpoint, String facebookAuthToken) throws FacebookSystemException;	
	
	public void updateMessageCount(long facebookAccountId);
	
	public void updateTaggedPhotos(long facebookAccountId);
	
	// FIXME CachedFacebookPhotoData should not be leaking out of its cache bean
	public void saveUpdatedTaggedPhotos(long facebookAccountId, List<? extends CachedFacebookPhotoData> cachedPhotos);

	public void removeExpiredTaggedPhotos(long facebookAccountId);
	
	public void updateAlbums(long facebookAccountId);
	
	public void handleExpiredSessionKey(long facebookAccountId);
	
	public void handleExpiredSessionKey(FacebookAccount facebookAccount);
}
