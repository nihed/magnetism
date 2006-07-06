package com.dumbhippo.server.impl;

import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.MySpaceTracker;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class ExternalAccountSystemBean implements ExternalAccountSystem {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(ExternalAccountSystemBean.class);
	
	@EJB
	IdentitySpider identitySpider;
	
	@EJB
	private MySpaceTracker mySpaceTracker;
	
	@EJB
	private MessageSender messageSender;	
	
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
		if (!em.contains(user.getAccount()))
			throw new RuntimeException("detached account in getExternalAccounts()");
		
		return user.getAccount().getExternalAccounts();
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
}
