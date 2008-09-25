package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;
import javax.persistence.EntityManager;

import com.dumbhippo.Site;
import com.dumbhippo.Thumbnail;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.UserViewpoint;

@DMO(classId="http://mugshot.org/p/o/externalAccount", resourceBase="/o/externalAccount")
public abstract class ExternalAccountDMO extends DMObject<ExternalAccountKey> {
	private ExternalAccount externalAccount;
	
	@EJB
	ExternalAccountSystem externalAccountSystem;
	
	@Inject
	EntityManager em;
			
	@Inject
	DMSession session;
	
	public ExternalAccountDMO(ExternalAccountKey key) {
		super(key);
	}

	@Override
	protected void init() throws NotFoundException {
		ExternalAccountKey key = getKey();
		User user = em.find(User.class, key.getUserId().toString());
		if (user == null)
		    throw new NotFoundException("No such user");
			
		externalAccount = externalAccountSystem.lookupExternalAccount(new UserViewpoint(user, Site.NONE), String.valueOf(key.getId()));
	}
		
	// FIXME: probably should add enum support and only convert to string when going to XML
	@DMProperty(defaultInclude=true)
	public String getAccountType() {
		return externalAccount.getAccountType().toString();
	}
	
	// FIXME: probably should add enum support and only convert to string when going to XML
	@DMProperty(defaultInclude=true)
	public String getSentiment() {
		return externalAccount.getSentiment().toString();
	}
	
	@DMProperty(defaultInclude=true)
	public String getQuip() {
		if (externalAccount.getSentiment() == Sentiment.HATE)
			return externalAccount.getQuip();
		else
			return null;
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getLink() {
		if (externalAccount.isLovedAndEnabled())
			return externalAccount.getLink();
		else
			return null;
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getIconUrl() {
		return "/images3/" + externalAccount.getIconName();
	}

	@DMProperty(defaultInclude=true)
	public String getUsername() {
		return externalAccount.getUsername();
	}
	
	@DMProperty
	public List<ThumbnailDMO> getThumbnails() {
		List<? extends Thumbnail> thumbnails = externalAccountSystem.getThumbnails(externalAccount);
		if (thumbnails == null)
			return Collections.emptyList();
		
		List<ThumbnailDMO> result = new ArrayList<ThumbnailDMO>();
		for (Thumbnail thumbnail : thumbnails)
			result.add(session.findUnchecked(ThumbnailDMO.class, ThumbnailDMO.getKey(externalAccount.getAccount().getOwner(), thumbnail)));
		
		return result;
	}
}
