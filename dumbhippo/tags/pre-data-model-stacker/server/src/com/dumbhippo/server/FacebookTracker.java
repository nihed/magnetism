package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.services.FacebookPhotoDataView;

@Local
public interface FacebookTracker {

	public void updateOrCreateFacebookAccount(UserViewpoint viewpoint, String facebookAuthToken) throws FacebookSystemException;	
	
	public void updateOrCreateFacebookAccount(UserViewpoint viewpoint, String sessionKey, String facebookUserId, Boolean applicationEnabled) throws FacebookSystemException;

	public User createNewUserWithFacebookAccount(String sessionKey, String facebookUserId, Boolean applicationEnabled) throws FacebookSystemException;

	public FacebookAccount getFacebookAccount(String facebookUserId);
	
	public void updateFbmlForUser(User user);
	
	public void publishUserAction(Block block, User user);
	
	public void updateMessageCount(long facebookAccountId);
	
	public void updateTaggedPhotos(long facebookAccountId);
	
	public void saveUpdatedTaggedPhotos(long facebookAccountId, List<? extends FacebookPhotoDataView> cachedPhotos);

	public void removeExpiredTaggedPhotos(long facebookAccountId);
	
	public void updateAlbums(long facebookAccountId);
	
	public void handleExpiredSessionKey(long facebookAccountId);
	
	public void handleExpiredSessionKey(FacebookAccount facebookAccount);
}
