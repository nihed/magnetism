package com.dumbhippo.web;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.live.LivePost;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.LiveUser;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.PersonView;

public class AdminPage extends AbstractSigninPage {

	protected static final Logger logger = GlobalSetup.getLogger(AdminPage.class);

	private Configuration config;
	
	private LiveState liveState;

	public AdminPage() throws HumanVisibleException {
		super();
		liveState = LiveState.getInstance();		
		config = WebEJBUtil.defaultLookup(Configuration.class);
		String isAdminEnabled = config.getProperty(HippoProperty.ENABLE_ADMIN_CONSOLE);
		logger.debug("checking whether admin console is enabled: " + isAdminEnabled);
		if (!isAdminEnabled.equals("true"))
			throw new HumanVisibleException("Administrator console not enabled");

	}
	
	public boolean isValid() throws HumanVisibleException {
		PersonView person = getPerson();
		return identitySpider.isAdministrator(person.getUser());
	}
	
	private Set<PersonView> liveUserSetToPersonView(Set<LiveUser> lusers) {
		Set<PersonView> result = new HashSet<PersonView>();

		for (LiveUser luser : lusers) {
			User user = identitySpider.lookupUser(luser);
			result.add(identitySpider.getSystemView(user));					
		}
		return result;		
	}

	public Set<PersonView> getCachedLiveUsers() {
		return liveUserSetToPersonView(liveState.getLiveUserCacheSnapshot());
	}
	
	public Set<PersonView> getAvailableLiveUsers() {
		return liveUserSetToPersonView(liveState.getLiveUserAvailableSnapshot());
	}	
	
	public Set<LivePost> getLivePosts() {
		return liveState.getLivePostSnapshot();
	}
}
