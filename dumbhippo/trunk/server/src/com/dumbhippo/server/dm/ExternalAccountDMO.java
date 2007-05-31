package com.dumbhippo.server.dm;

import javax.ejb.EJB;
import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.SystemViewpoint;

@DMO(classId="http://mugshot.org/p/o/externalAccount", resourceBase="/o/externalAccount")
public abstract class ExternalAccountDMO extends DMObject<ExternalAccountKey> {
	private ExternalAccount externalAccount;
	
	@EJB
	ExternalAccountSystem externalAccountSystem;
	
	@Inject
	EntityManager em;
			
	public ExternalAccountDMO(ExternalAccountKey key) {
		super(key);
	}

	@Override
	protected void init() throws NotFoundException {
		ExternalAccountKey key = getKey();
		
		long id = key.getId();
		if (id >= 0) {
			externalAccount = em.find(ExternalAccount.class, id);
		} else {
			User user = em.find(User.class, key.getUserId().toString());
			if (user == null)
				throw new NotFoundException("No such user");
			
			externalAccount = externalAccountSystem.lookupExternalAccount(SystemViewpoint.getInstance(), user, key.getType());
		}
	}
		
	public ExternalAccountType getAccountType() {
		return externalAccount.getAccountType();
	}
	
	public String getQuip() {
		return externalAccount.getQuip();
	}
	
	public Sentiment getSentiment() {
		return externalAccount.getSentiment();
	}
	
	public String getLink() {
		if (externalAccount.isLovedAndEnabled())
			return externalAccount.getLink();
		else
			return null;
	}
}
