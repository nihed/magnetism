package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.WantsInSystem;
import com.dumbhippo.server.WantsInView;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.WebEJBUtil;


public class InvitationAdminPage extends AbstractSigninRequiredPage {

	protected static final Logger logger = GlobalSetup.getLogger(AdminPage.class);

	private Configuration config;
	
	private IdentitySpider identitySpider;
	private AccountSystem accountSystem;
	private WantsInSystem wantsInSystem;
	private GroupSystem groupSystem;
	
	private ListBean<WantsInView> wantsInList;
	private int countToInvite;

	private ListBean<GroupView> groups;
	
	public InvitationAdminPage() throws HumanVisibleException {
		super();
		config = WebEJBUtil.defaultLookup(Configuration.class);
		String isAdminEnabled = config.getProperty(HippoProperty.ENABLE_ADMIN_CONSOLE);
		logger.debug("admin console enabled: {}", isAdminEnabled);
		if (!isAdminEnabled.equals("true"))
			throw new HumanVisibleException("Administrator console not enabled");
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		accountSystem = WebEJBUtil.defaultLookup(AccountSystem.class);		
		wantsInSystem = WebEJBUtil.defaultLookup(WantsInSystem.class);	
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		countToInvite = 50;
	}
	
	public boolean isValid() throws HumanVisibleException {
		PersonView person = getPerson();
		return identitySpider.isAdministrator(person.getUser());
	}
	
	public int getWantsInCount() {
		return wantsInSystem.getWantsInCount();
	}
	
	public ListBean<WantsInView> getWantsInList() {
		if (wantsInList == null) {
			wantsInList = new ListBean<WantsInView>(wantsInSystem.getWantsInViewsWithoutInvites(countToInvite));
		}
		return wantsInList;
	}

	public int getCountToInvite() {
		return countToInvite;
	}	
	
	public void setCountToInvite(int countToInvite) {
		this.countToInvite = countToInvite;
	}
	
	public ListBean<GroupView> getGroups() {
		if (groups == null) {
		    Character character = Character.MUGSHOT;
		    User inviter = accountSystem.getCharacter(character);
			groups = new ListBean<GroupView>(GroupView.sortedList(groupSystem.findGroups(new UserViewpoint(inviter), inviter, MembershipStatus.ACTIVE)));
		}
		return groups;		
	}
}