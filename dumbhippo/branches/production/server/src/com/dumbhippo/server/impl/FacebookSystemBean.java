package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.FacebookEventType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class FacebookSystemBean implements FacebookSystem {
	static private final Logger logger = GlobalSetup.getLogger(FacebookSystemBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 
	
	@EJB
	private ExternalAccountSystem externalAccounts;
	
	@EJB
	private Configuration config;
	
	public FacebookAccount lookupFacebookAccount(Viewpoint viewpoint, User user) throws NotFoundException {
		if (!em.contains(user.getAccount()))
			throw new RuntimeException("detached account in lookupExternalAccount()");
	
		ExternalAccount externalAccount = externalAccounts.lookupExternalAccount(viewpoint, user, ExternalAccountType.FACEBOOK);

        return lookupFacebookAccount(externalAccount);
	}
	
	private FacebookAccount lookupFacebookAccount(ExternalAccount externalAccount) throws NotFoundException {
		FacebookAccount facebookAccount;
		if (externalAccount.getExtra() == null) {
			throw new NotFoundException("No facebook account details for user " + externalAccount.getAccount().getOwner());
		} else {
			facebookAccount = em.find(FacebookAccount.class, Long.parseLong(externalAccount.getExtra()));
			if (facebookAccount == null)
				throw new RuntimeException("Invalid FacebookAccount id " + externalAccount.getExtra() + " is stored in externalAccount " + externalAccount);
		}	
		
		return facebookAccount;
	}
	
	public List<FacebookEvent> getLatestEvents(Viewpoint viewpoint, FacebookAccount facebookAccount, int eventsCount) {
		ArrayList<FacebookEvent> list = new ArrayList<FacebookEvent>();
		
		boolean viewpointIsOfOwner = viewpoint.isOfUser(facebookAccount.getExternalAccount().getAccount().getOwner());
		
		for (FacebookEvent event : facebookAccount.getFacebookEvents()) {
			if (event.getEventType().shouldDisplay(viewpointIsOfOwner)) {
                list.add(event);
			}
		}
		
		if (FacebookEventType.UNREAD_MESSAGES_UPDATE.shouldDisplay(viewpointIsOfOwner) &&
			facebookAccount.getMessageCountTimestampAsLong() > 0) {
	        list.add(new FacebookEvent(facebookAccount, FacebookEventType.UNREAD_MESSAGES_UPDATE, 
	      		                       facebookAccount.getUnreadMessageCount(), facebookAccount.getMessageCountTimestampAsLong()));
		}
		
		if (FacebookEventType.UNSEEN_POKES_UPDATE.shouldDisplay(viewpointIsOfOwner) &&
			facebookAccount.getPokeCountTimestampAsLong() > 0) {
	        list.add(new FacebookEvent(facebookAccount, FacebookEventType.UNSEEN_POKES_UPDATE, 
        		                       facebookAccount.getUnseenPokeCount(), facebookAccount.getPokeCountTimestampAsLong()));		
		}
		
		// we want newer(greater) timestamps to be in the front of the list
		Collections.sort(list, new Comparator<FacebookEvent>() {
			public int compare (FacebookEvent fe1, FacebookEvent fe2) {
				if (fe1.getEventTimestampAsLong() < fe2.getEventTimestampAsLong())
					return 1;
				else if (fe1.getEventTimestampAsLong() > fe2.getEventTimestampAsLong())
					return -1;
				else
					return 0;
			}
		});
		
		return list.subList(0, Math.min(eventsCount, list.size()));
	}
	
	public String getProfileLink(ExternalAccount externalAccount) {
		try {
		    FacebookAccount facebookAccount = lookupFacebookAccount(externalAccount);
		    return "http://www.facebook.com/profile.php?uid=" 
		           + facebookAccount.getFacebookUserId() + "&api_key=" + getApiKey();
		} catch (NotFoundException e) {
			throw new RuntimeException("An ExternalAccount passed to getProfileLink() was not associated with a valid FacebookAccount.", e);
		}
	}
	
	
	public String getApiKey() {
    	String apiKey;
		try {
			apiKey = config.getPropertyNoDefault(HippoProperty.FACEBOOK_API_KEY).trim();
			if (apiKey.length() == 0)
				apiKey = null;	
		} catch (PropertyNotFoundException e) {
			apiKey = null;
		}
		
		if (apiKey == null)
			logger.warn("Facebook API key is not set, we can't use Facebook web services.");
		
		return apiKey;
	}
	
}
