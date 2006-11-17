package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.CachedFacebookPhotoData;
import com.dumbhippo.server.views.UserViewpoint;

@Local
public interface FacebookTracker {

	public void updateOrCreateFacebookAccount(UserViewpoint viewpoint, String facebookAuthToken);	
	
	public void updateMessageCount(long facebookAccountId);
	
	public void updateTaggedPhotos(long facebookAccountId);
	
	public void saveUpdatedTaggedPhotos(long facebookAccountId, List<CachedFacebookPhotoData> cachedPhotos);

	public void removeExpiredTaggedPhotos(long facebookAccountId);
	
	public void updateAlbums(long facebookAccountId);
	
	public void handleExpiredSessionKey(long facebookAccountId);
}
