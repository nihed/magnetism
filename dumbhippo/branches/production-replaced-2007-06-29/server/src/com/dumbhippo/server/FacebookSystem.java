package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.Viewpoint;

/**
 * We can't have a circular dependency between two service beans, such as the FacebookTrackerBean
 * and StackerBean. Therefore we have these methods separate from the methods in the FacebookTracker, 
 * so that we can implement them in the stateless FacebookSystemBean and use them in the StackerBean.
 * 
 * @author marinaz
 * 
 */
@Local
public interface FacebookSystem {
	
	public List<FacebookAccount> getAllAccounts();
	
	public FacebookAccount lookupFacebookAccount(Viewpoint viewpoint, String userId) throws ParseException, NotFoundException;

	public FacebookAccount lookupFacebookAccount(Viewpoint viewpoint, User user) throws NotFoundException;
	
	public FacebookEvent lookupFacebookEvent(Viewpoint viewpoint, long eventId) throws NotFoundException;
	
	public List<FacebookEvent> getLatestEvents(Viewpoint viewpoint, FacebookAccount facebookAccount, int eventsCount);
	
	public String getProfileLink(ExternalAccount externalAccount);
	
	public String getEventLink(FacebookEvent facebookEvent);

	public String getApiKey();	
	
	// this is a migration function needed for moving from using Facebook beta to using Facebook 1.0 API
	public void decodeUserIds();
	
	public void updateUserIds(List<FacebookAccount> detachedFacebookAccounts);
	
}
