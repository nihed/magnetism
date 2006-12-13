package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.server.views.UserViewpoint;

@Local
public interface FacebookTracker {

	public void updateOrCreateFacebookAccount(UserViewpoint viewpoint, String facebookAuthToken);
	
	public List<FacebookAccount> getAccountsWithValidSession();
	
	public void updateMessageCount(long facebookAccountId);
	
	public void updateTaggedPhotos(long facebookAccountId);
	
	public void updateAlbums(long facebookAccountId);
}
