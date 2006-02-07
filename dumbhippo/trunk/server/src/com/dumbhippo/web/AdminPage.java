package com.dumbhippo.web;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.LiveUser;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;

public class AdminPage {

	protected static final Logger logger = GlobalSetup.getLogger(AdminPage.class);

	private Configuration config;
	private IdentitySpider identitySpider;
	
	private LiveState liveState;

	public AdminPage() throws HumanVisibleException {
		liveState = LiveState.getInstance();		
		config = WebEJBUtil.defaultLookup(Configuration.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		String isAdminEnabled = config.getProperty(HippoProperty.ENABLE_ADMIN_CONSOLE);
		logger.debug("checking whether admin console is enabled: " + isAdminEnabled);
		if (!isAdminEnabled.equals("true"))
			throw new HumanVisibleException("Administrator console not enabled");
	}

	public Set<PersonView> getLiveUsers() {
		Set<LiveUser> lusers = liveState.getLiveUserSnapshot();
		Set<PersonView> ret = new HashSet<PersonView>();

		for (LiveUser luser : lusers) {
			User user;
			try {
				user = identitySpider.lookupGuid(User.class, luser.getUserId());
			} catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
			ret.add(identitySpider.getSystemView(user));					
		}
		return ret;
	}
}
