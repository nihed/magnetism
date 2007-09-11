package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.persistence.EntityManager;

import com.dumbhippo.Site;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.DesktopSetting;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.XmppResource;
import com.dumbhippo.server.DesktopSettings;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.server.views.UserViewpoint;

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
	
	@EJB
	private DesktopSettings settings;
	
	@EJB
	private ApplicationSystem applicationSystem;
	
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

	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getHomeUrl() {
		return "/person?who=" + user.getId();
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getPhotoUrl() {
		return user.getPhotoUrl();
	}
	
	@DMProperty
	public List<ExternalAccountDMO> getLovedAccounts() {
		List<ExternalAccountDMO> result = new ArrayList<ExternalAccountDMO>();
		
		for (ExternalAccount externalAccount : user.getAccount().getExternalAccounts()) {
			if (externalAccount.isLovedAndEnabled())
				result.add(session.findUnchecked(ExternalAccountDMO.class, new ExternalAccountKey(externalAccount)));
		}
		
		return result;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeeFriendsOnly(this)")
	public Set<UserDMO> getContacts() {
		Set<UserDMO> result = new HashSet<UserDMO>();
		
		for (Guid guid : identitySpider.computeContacts(user.getGuid()))
			result.add(session.findUnchecked(UserDMO.class, guid));
		
		return result;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeePrivate(this)")
	public Set<UserDMO> getContacters() {
		Set<UserDMO> result = new HashSet<UserDMO>();
		
		for (Guid guid : identitySpider.computeContacters(user.getGuid()))
			result.add(session.findUnchecked(UserDMO.class, guid));
		
		return result;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeeFriendsOnly(this)")
	public String getEmail() {
		// FIXME: We need to let the user select their "primary" email,
		// rather than returning a hash-table random email
		for (AccountClaim ac : user.getAccountClaims()) {
			Resource r = ac.getResource();
			if (r instanceof EmailResource)
				return ((EmailResource)r).getEmail();
		}
		
		return null;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeeFriendsOnly(this)")
	public String getAim() {
		for (AccountClaim ac : user.getAccountClaims()) {
			Resource r = ac.getResource();
			if (r instanceof AimResource)
				return ((AimResource)r).getScreenName();
		}
		
		return null;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeeFriendsOnly(this)")
	public String getXmpp() {
		for (AccountClaim ac : user.getAccountClaims()) {
			Resource r = ac.getResource();
			if (r instanceof XmppResource)
				return ((XmppResource)r).getJid();
		}
		
		return null;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeePrivate(this)")
	public Set<DesktopSettingDMO> getSettings() {
		Set<DesktopSettingDMO> result = new HashSet<DesktopSettingDMO>();
		
		Collection<DesktopSetting> userSettings = settings.getSettingsObjects(user); 
		
		for (DesktopSetting setting : userSettings) {
			result.add(session.findUnchecked(DesktopSettingDMO.class, new DesktopSettingKey(setting)));
		}
		
		return result;
	}

	// the Date here can be null, not sure if that's OK
	@DMProperty
	@DMFilter("viewer.canSeePrivate(this)")
	public long getApplicationUsageStart() {
		Date since = applicationSystem.getMyApplicationUsageStart(new UserViewpoint(user, Site.NONE));
		return since == null ? -1 : since.getTime();
	}
	
	@DMProperty
	@DMFilter("viewer.canSeePrivate(this)")
	public boolean getApplicationUsageEnabled() {
		return identitySpider.getApplicationUsageEnabled(user);
	}
	
	@DMProperty
	@DMFilter("viewer.canSeePrivate(this)")
	public Set<ApplicationDMO> getTopApplications() {
		UserViewpoint viewpoint = new UserViewpoint(user, Site.NONE);
		
		Set<ApplicationDMO> result = new HashSet<ApplicationDMO>();
	
		// returned "since" here can be null, which is OK
		Date since = applicationSystem.getMyApplicationUsageStart(viewpoint);

		List<String> appIds = applicationSystem.getMyMostUsedApplicationIds(viewpoint, since, 15);
		for (String appId : appIds) {
			result.add(session.findUnchecked(ApplicationDMO.class, appId));
		}
		
		return result;
	}
}
