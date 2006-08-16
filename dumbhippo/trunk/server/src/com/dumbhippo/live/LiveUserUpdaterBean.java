package com.dumbhippo.live;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SystemViewpoint;

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
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	private void initializeGroups(LiveUser user) {
		User dbUser = identitySpider.lookupUser(user);		
		user.setGroupCount(groupSystem.findGroupsCount(SystemViewpoint.getInstance(), dbUser, MembershipStatus.ACTIVE));
	}
	
	private void initializePostCount(LiveUser user) {
		User dbUser = identitySpider.lookupUser(user);		
		user.setSentPostsCount(postingBoard.getPostsForCount(SystemViewpoint.getInstance(), dbUser));		
	}
	
	private void initializeContactResources(LiveUser user) {
		User dbUser = identitySpider.lookupUser(user);		
		
		List l = em.createQuery("SELECT cc.resource.id FROM ContactClaim cc WHERE cc.account = :account")
			.setParameter("account", dbUser.getAccount())
			.getResultList();

		Set<Guid> guids = new HashSet<Guid>();

		try {
			for (Object o : l) {
				guids.add(new Guid((String)o));
			}
		} catch (ParseException e) {
			throw new RuntimeException("Database contained a bad GUID");
		}
	
		user.setContactResources(guids);
	}
	
	public void initialize(LiveUser user) {
		initializeGroups(user);
		initializePostCount(user);
		initializeContactResources(user);
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
				initializeContactResources(liveUser);
			} finally {
				state.updateLiveUser(liveUser);
			}
		}
	}
}