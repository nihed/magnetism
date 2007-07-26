package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.WantsInSystem;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.server.views.WantsInView;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.WebEJBUtil;


public class InvitationAdminPage extends AbstractSigninRequiredPage {

	protected static final Logger logger = GlobalSetup.getLogger(AdminPage.class);

	private Configuration config;
	
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
			Viewpoint viewpoint = getSignin().getViewpoint();
		    User inviter = accountSystem.getSiteCharacter(viewpoint.getSite());
			groups = new ListBean<GroupView>(GroupView.sortedList(groupSystem.findGroups(viewpoint, inviter, MembershipStatus.ACTIVE)));
		}
		return groups;		
	}
}