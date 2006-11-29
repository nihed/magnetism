package com.dumbhippo.live;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.views.SystemViewpoint;

// Implementation of LiveUserUpdater
@Stateless
public class LiveUserUpdaterBean implements LiveUserUpdater {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveUserUpdaterBean.class);
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	private PostingBoard postingBoard;
	
	private void initializeGroups(LiveUser user) {
		User dbUser = identitySpider.lookupUser(user);		
		user.setGroupCount(groupSystem.findGroupsCount(SystemViewpoint.getInstance(), dbUser, MembershipStatus.ACTIVE));
	}
	
	private void initializePostCount(LiveUser user) {
		User dbUser = identitySpider.lookupUser(user);		
		user.setSentPostsCount(postingBoard.getPostsForCount(SystemViewpoint.getInstance(), dbUser));		
	}
	
	private void initializeContactsCount(LiveUser user) {
		User dbUser = identitySpider.lookupUser(user);		
		user.setContactsCount(identitySpider.computeContactsCount(dbUser));		
	}
	
	public void initialize(LiveUser user) {
		initializeGroups(user);
		initializePostCount(user);
		initializeContactsCount(user);
	}
	
	public void handleGroupMembershipChanged(Guid userGuid) {
		LiveState state = LiveState.getInstance();
		LiveUser liveUser = state.peekLiveUserForUpdate(userGuid);
		if (liveUser != null) {
			try {
				initializeGroups(liveUser);
			} finally {
				state.updateLiveUser(liveUser);
			}
		}
	}
	
	public void handlePostCreated(Guid userGuid) {
		LiveState state = LiveState.getInstance();
		LiveUser liveUser = state.peekLiveUserForUpdate(userGuid);
		if (liveUser != null) {
			try {
				initializePostCount(liveUser);
			} finally {
				state.updateLiveUser(liveUser);
			}
		}
	}	

	public void handleContactsChanged(Guid userGuid) {
		LiveState state = LiveState.getInstance();
		LiveUser liveUser = state.peekLiveUserForUpdate(userGuid);
		if (liveUser != null) {
			try {
				initializeContactsCount(liveUser);
			} finally {
				state.updateLiveUser(liveUser);
			}
		}
	}	
}