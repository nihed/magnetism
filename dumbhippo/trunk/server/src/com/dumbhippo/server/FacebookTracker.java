package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.User;

@Local
public interface FacebookTracker {

	public void updateOrCreateFacebookAccount(UserViewpoint viewpoint, String facebookAuthToken);
	
	public FacebookAccount lookupFacebookAccount(Viewpoint viewpoint, User user) throws NotFoundException;
	
	public List<FacebookAccount> getAccountsWithValidSession();
	
	public void updateMessageCount(long facebookAccountId);
	
	public void updatePhotoData(long facebookAccountId);
	
	public List<FacebookEvent> getLatestEvents(Viewpoint viewpoint, FacebookAccount facebookAccount, int eventsCount);
	
	public String getApiKey();	
}
