package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.FacebookEventType;
import com.dumbhippo.persistence.FacebookResource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class FacebookSystemBean implements FacebookSystem {
	static private final Logger logger = GlobalSetup.getLogger(FacebookSystemBean.class);
	
	// how long to wait on the Facebook API call
	static protected final int REQUEST_TIMEOUT = 1000 * 12;

	// how long we wait after the person logs out with the client or is last seen being active 
	// on the web to stop frequent requests to Facebook, in milliseconds
	static protected final int FREQUENT_REQUESTS_GRACE_PERIOD = 24 * 60 * 60 * 1000; // 24 hours
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 
	
	@EJB
	private ExternalAccountSystem externalAccounts;
	
	@EJB
	private Configuration config;
	
	@EJB
	private IdentitySpider identitySpider;
	
	public List<FacebookAccount> getAllAccounts() {
		List<?> list = em.createQuery("SELECT fa FROM FacebookAccount fa").getResultList();
		return TypeUtils.castList(FacebookAccount.class, list);
	}

	public List<FacebookAccount> getValidAccounts(boolean applyLoginConstraints) {
		String loginConstraints = "";
		if (applyLoginConstraints) {
			loginConstraints = " AND (a.lastLoginDate > a.lastLogoutDate OR " +
                               "(a.lastLoginDate IS NOT NULL AND a.lastLogoutDate IS NULL) OR " +
                               "a.lastLogoutDate >= :tooLongAgo OR a.lastWebActivityDate >= :tooLongAgo)";
		}
		
		Query q = em.createQuery("SELECT fa FROM FacebookAccount fa, ExternalAccount ea, Account a " +
				                 "WHERE fa.sessionKeyValid = true AND fa.externalAccount = ea " +
				                 "AND ea.account = a" + loginConstraints);
         
		if (applyLoginConstraints) {		                   
		    q.setParameter("tooLongAgo",  new Date(System.currentTimeMillis() - FREQUENT_REQUESTS_GRACE_PERIOD));
		}

		return TypeUtils.castList(FacebookAccount.class, q.getResultList());
	}
	
	
	
	public FacebookAccount lookupFacebookAccount(Viewpoint viewpoint, String userId) throws ParseException, NotFoundException {
        User user = EJBUtil.lookupGuid(em, User.class, new Guid(userId));
        return lookupFacebookAccount(viewpoint, user);
	}
	
	public FacebookAccount lookupFacebookAccount(Viewpoint viewpoint, User user) throws NotFoundException {
		if (!em.contains(user))
			user = EJBUtil.lookupGuid(em, User.class, user.getGuid());
	
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
	
	public FacebookEvent lookupFacebookEvent(Viewpoint viewpoint, long eventId) throws NotFoundException {
		FacebookEvent facebookEvent = em.find(FacebookEvent.class, eventId);
		
		// before we implement getting info about one's friends and network, we can only show
		// facebook blocks to their owners; later we can return it if the viewpoint is for someone
		// who is the owner's facebook friend or is in the same network with the owner and
		// facebookEvent.getEventType().getDisplayToOthers() is true
		if (viewpoint.isOfUser(facebookEvent.getFacebookAccount().getExternalAccount().getAccount().getOwner())) {
			return facebookEvent;
		} else {
			throw new NotFoundException("Viewpoint " + viewpoint + " can't view facebook event " + facebookEvent);
		}
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
		    return "http://www.facebook.com/profile.php?id=" 
		           + facebookAccount.getFacebookUserId() + "&api_key=" + getApiKey();
		} catch (NotFoundException e) {
			throw new RuntimeException("An ExternalAccount passed to getProfileLink() was not associated with a valid FacebookAccount.", e);
		}
	}
	
	public String getEventLink(FacebookEvent facebookEvent) {
        if ((facebookEvent.getEventType() == FacebookEventType.LOGIN_STATUS_EVENT) && (facebookEvent.getCount() <= 0)) {
        	// facebookEvent.getCount() = -1 is a special event for prompting the user to save their Facebook login info
        	if (facebookEvent.getCount() == 0)
	            return "http://api.facebook.com/login.php?api_key=" + getApiKey() + "&v=1.0&next=?next=home";
        	else 
        		return "http://api.facebook.com/login.php?api_key=" + getApiKey() + "&v=1.0&skipcookie=1&next=?next=home";
	    } else {
		    return "http://www.facebook.com/" + facebookEvent.getEventType().getPageName() +  ".php?uid=" 
	               + facebookEvent.getFacebookAccount().getFacebookUserId();
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
	
	/*
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void decodeUserIds() {
		FacebookSystem facebookSystem = EJBUtil.defaultLookup(FacebookSystem.class);
		List<FacebookAccount> facebookAccounts = facebookSystem.getAllAccounts();
		
        FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		
		ws.decodeUserIds(facebookAccounts);
		
		// those are detached copies of Facebook accounts, we need to set facebookUserId on attached copy
		facebookSystem.updateUserIds(facebookAccounts);
	}
	
	public void updateUserIds(List<FacebookAccount> detachedFacebookAccounts) {
		for (FacebookAccount detachedFacebookAccount : detachedFacebookAccounts) {
		    FacebookAccount facebookAccount = em.find(FacebookAccount.class, detachedFacebookAccount.getId());
		    // just a sanity check
		    if (detachedFacebookAccount.getFacebookUserId() != null) {
		        facebookAccount.setFacebookUserId(detachedFacebookAccount.getFacebookUserId());
		    } else {
		    	logger.warn("Web services produced a null Facebook user id for Facebook account {}",  
		    			detachedFacebookAccount.getId());
		    }
		} 
	}
	*/

	public void createFacebookResources() {
		List<FacebookAccount> facebookAccounts = getAllAccounts();
		for (FacebookAccount facebookAccount : facebookAccounts) {
			Query q = em.createQuery("from FacebookResource f where f.facebookUserId = :facebookUserId");
			q.setParameter("facebookUserId", facebookAccount.getFacebookUserId());

			FacebookResource res;
			try {
				res = (FacebookResource) q.getSingleResult();
				logger.warn("resource {} already existed when we were running createFacebookResources", res);
			} catch (NoResultException e) {
				res = new FacebookResource(facebookAccount.getFacebookUserId());
				em.persist(res);
				identitySpider.addVerifiedOwnershipClaim(facebookAccount.getExternalAccount().getAccount().getOwner(), res);
			}
		}
	}
}
