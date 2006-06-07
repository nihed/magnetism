package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.WantsInSystem;
import com.dumbhippo.server.WantsInView;


public class InvitationAdminPage extends AbstractSigninRequiredPage {

	protected static final Logger logger = GlobalSetup.getLogger(AdminPage.class);

	private Configuration config;
	
	private IdentitySpider identitySpider;
	private WantsInSystem wantsInSystem;
	
	private ListBean<WantsInView> wantsInList;
	private int countToInvite;
	
    public InvitationAdminPage() throws HumanVisibleException {
		super();
		config = WebEJBUtil.defaultLookup(Configuration.class);
		String isAdminEnabled = config.getProperty(HippoProperty.ENABLE_ADMIN_CONSOLE);
		logger.debug("admin console enabled: {}", isAdminEnabled);
		if (!isAdminEnabled.equals("true"))
			throw new HumanVisibleException("Administrator console not enabled");
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		wantsInSystem = WebEJBUtil.defaultLookup(WantsInSystem.class);	
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
	
}