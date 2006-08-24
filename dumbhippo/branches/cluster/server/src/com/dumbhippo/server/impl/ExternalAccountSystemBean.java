package com.dumbhippo.server.impl;

import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.annotation.IgnoreDependency;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Thumbnail;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.MySpaceTracker;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.services.FlickrPhotoSize;
import com.dumbhippo.services.FlickrPhotos;
import com.dumbhippo.services.FlickrWebServices;

@Stateless
public class ExternalAccountSystemBean implements ExternalAccountSystem {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(ExternalAccountSystemBean.class);
	
	@EJB
	@IgnoreDependency
	private MySpaceTracker mySpaceTracker;
	
	@EJB
	@IgnoreDependency
	private MessageSender messageSender;	
	
	@EJB
	private Configuration config;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 
	
	public ExternalAccount getOrCreateExternalAccount(UserViewpoint viewpoint, ExternalAccountType type) {
		Account a = viewpoint.getViewer().getAccount();
		if (!em.contains(a))
			throw new RuntimeException("detached account in getOrCreateExternalAccount");
		
		ExternalAccount external = a.getExternalAccount(type);
		if (external == null) {
			external = new ExternalAccount(type);
			external.setAccount(a);
			em.persist(external);
			a.getExternalAccounts().add(external);
		}
		return external;
	}

	public ExternalAccount lookupExternalAccount(Viewpoint viewpoint, User user, ExternalAccountType type)
		throws NotFoundException {
		if (!em.contains(user.getAccount()))
			throw new RuntimeException("detached account in lookupExternalAccount()");
		
		// Right now, external accounts are public, unlike email/aim resources which are friends only...
		// so we don't need to use the viewpoint. But here in case we want to add it later.
		ExternalAccount external = user.getAccount().getExternalAccount(type);
		if (external == null)
			throw new NotFoundException("No external account of type " + type + " for user " + user);
		else
			return external;
	}
	
	public Set<ExternalAccount> getExternalAccounts(Viewpoint viewpoint, User user) {
		// Right now we ignore the viewpoint, so this method is pretty pointless.
		// but if people use it, future code will work properly.
		
		// be sure the account is attached... the external accounts are lazy-loaded
		if (!em.contains(user.getAccount()))
			throw new RuntimeException("detached account in getExternalAccounts()");
		
		Set<ExternalAccount> accounts = user.getAccount().getExternalAccounts();
		//logger.debug("{} external accounts for user {}", accounts.size(), user);
		return accounts;
	}
	
	public void setMySpaceName(UserViewpoint viewpoint, String name) throws ValidationException {
		ExternalAccount external = getOrCreateExternalAccount(viewpoint, ExternalAccountType.MYSPACE);
		external.setHandleValidating(name);
		mySpaceTracker.updateFriendId(viewpoint.getViewer());
		messageSender.sendMySpaceNameChangedNotification(viewpoint.getViewer());
	}
	
	public String getMySpaceName(Viewpoint viewpoint, User user) throws NotFoundException {
		ExternalAccount external = lookupExternalAccount(viewpoint, user, ExternalAccountType.MYSPACE);
		if (external.getSentiment() == Sentiment.LOVE &&
				external.getHandle() != null) {
			return external.getHandle();
		} else {
			throw new NotFoundException("No MySpace name for user " + user);
		}
	}
	
	private void loadFlickrThumbnails(Viewpoint viewpoint, ExternalAccount account) {
		if (account.getAccountType() != ExternalAccountType.FLICKR)
			throw new IllegalArgumentException("should be a flickr account here");
		
		if (account.getHandle() == null || account.getSentiment() != Sentiment.LOVE)
			return;
		
		FlickrWebServices ws = new FlickrWebServices(5000, config);
		FlickrPhotos photos = ws.lookupPublicPhotos(account.getHandle(), 1);
		if (photos == null) {
			logger.debug("Failed to load public photos for {}", account);
			return;
		}
		
		if (photos.getPhotos().size() > 0) {
			account.setThumbnails(TypeUtils.castList(Thumbnail.class, photos.getPhotos()), photos.getTotal(), 
					FlickrPhotoSize.SMALL_SQUARE.getPixels(), FlickrPhotoSize.SMALL_SQUARE.getPixels());
		}
	}
	
	public void loadThumbnails(Viewpoint viewpoint, Set<ExternalAccount> accounts) {
		for (ExternalAccount external : accounts) {
			switch (external.getAccountType()) {
			case FLICKR:
				loadFlickrThumbnails(viewpoint, external);
				break;
			default:
				break;
			}
		}
	}
}
