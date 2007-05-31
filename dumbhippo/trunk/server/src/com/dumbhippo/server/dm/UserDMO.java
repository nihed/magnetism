package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/user", resourceBase="/o/user")
public abstract class UserDMO extends DMObject<Guid> {
	private User user;
//	private String email;
//	private String aim;
//	private boolean contactOfViewer;
	
	@Inject
	private EntityManager em;
	
	@Inject
	private DMSession session;

	@EJB
	private IdentitySpider identitySpider;
	
	protected UserDMO(Guid key) {
		super(key);
	}
	
	@Override
	protected void init() throws NotFoundException {
		user = em.find(User.class, getKey().toString());
		if (user == null)
			throw new NotFoundException("No such user");
	}
	
	@DMProperty(defaultInclude=true)
	public String getName() {
		return user.getNickname();
	}

	@DMProperty(defaultInclude=true)
	public String getHomeUrl() {
		return "/person?who=" + user.getId();
	}
	
	@DMProperty(defaultInclude=true)
	public String getPhotoUrl() {
		return user.getPhotoUrl();
	}
	
	@DMProperty
	public List<ExternalAccountDMO> getExternalAccounts() {
		List<ExternalAccountDMO> result = new ArrayList<ExternalAccountDMO>();
		
		for (ExternalAccount externalAccount : user.getAccount().getExternalAccounts()) {
			if (externalAccount.isLovedAndEnabled())
				result.add(session.findUnchecked(ExternalAccountDMO.class, new ExternalAccountKey(externalAccount)));
		}
		
		return result;
	}
	
	@DMProperty
	public Set<UserDMO> getContacts() {
		Set<UserDMO> result = new HashSet<UserDMO>();
		
		for (Guid guid : identitySpider.computeContacts(user.getGuid()))
			result.add(session.findUnchecked(UserDMO.class, guid));
		
		return result;
	}
	
	@DMProperty
	public Set<UserDMO> getContacters() {
		Set<UserDMO> result = new HashSet<UserDMO>();
		
		for (Guid guid : identitySpider.computeContacters(user.getGuid()))
			result.add(session.findUnchecked(UserDMO.class, guid));
		
		return result;
	}
}
