package com.dumbhippo.live;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.UserViewpoint;

// Implementation of LiveUserUpdater
@Stateless
public class LiveUserUpdaterBean implements LiveUserUpdater {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveUserUpdaterBean.class);
	
	@EJB
	IdentitySpider identitySpider;
	
	@EJB
	GroupSystem groupSystem;
	
	@EJB
	PostingBoard postingBoard;
	
	@PersistenceContext(unitName = "dumbhippo")
	EntityManager em;
	
	@EJB
	MessageSender msgSender;
	
	static final int MAX_ACTIVE_POSTS = 3;
	
	static final int RECENT_POSTS_MAX_HISTORY = 20;
	
	static final int CURRENT_POSTS_SEC = 60 * 60;
	
	private boolean postIsCurrent(PostView post) {
		Date postDate = post.getPost().getPostDate();
		Date cur = new Date();
		long timeDiff = cur.getTime() - postDate.getTime();
		// Should probably push this into DB query
		return timeDiff < (CURRENT_POSTS_SEC * 1000);		
	}
	
	private List<PostView> getRecentPosts(LiveUser user) {
		User dbUser = identitySpider.lookupUser(user);
		List<PostView> posts = postingBoard.getReceivedPosts(new UserViewpoint(dbUser), dbUser, 0, RECENT_POSTS_MAX_HISTORY);
		logger.debug("Got {} for getReceivedPosts for user {}", posts.size(), user.getGuid());
		return posts;
	}

	private double computeInitialTemperature(LiveUser user, List<PostView> posts) {
		double score = 0.0;	
		for (PostView post : posts) {
			// Look for max of 3 unviewed posts			
			if (score >= 3.0)
				break;
			 if (postIsCurrent(post) && !post.isViewerHasViewed())
				 score += 1.0;
		}
		return score;
	}
	
	// This probably needs to scale dynamically somehow based on
	// past hotness.
	private Hotness hotnessFromScore(LiveUser user, double score) {
		if (score < 1.0) {
			return Hotness.COLD;
		} else if (score < 4.0) {
			return Hotness.COOL;
		} else if (score < 8.0) {
			return Hotness.WARM;
		} else if (score < 16.0) {
			return Hotness.GETTING_HOT;
		} else {
			return Hotness.HOT;
		}
	}
	
	private void initializeGroups(LiveUser user) {
		User dbUser = identitySpider.lookupUser(user);		
		user.setGroupCount(groupSystem.findGroupsCount(SystemViewpoint.getInstance(), dbUser, MembershipStatus.ACTIVE));
	}
	
	private void initializePostCount(LiveUser user) {
		User dbUser = identitySpider.lookupUser(user);		
		user.setSentPostsCount(postingBoard.getPostsForCount(SystemViewpoint.getInstance(), dbUser));		
	}
	
	public void initialize(LiveUser user) {
		initializeFromPosts(user, getRecentPosts(user));
		initializeGroups(user);
		initializePostCount(user);
	}
	
	private void initializeFromPosts(LiveUser user, List<PostView> recentPosts) {
		LiveState state = LiveState.getInstance();		
		double score = computeInitialTemperature(user, recentPosts);
		Hotness hotness = hotnessFromScore(user, score);
		user.setHotness(hotness);
		List<Guid> activePosts = new ArrayList<Guid>();
		List<LivePost> livePosts = new ArrayList<LivePost>();
		for (PostView post : recentPosts) {
			livePosts.add(state.getLivePost(post.getPost().getGuid()));
		}
		// First add in all the posts with active chats
		for (LivePost post : livePosts) {
			if (activePosts.size() >= MAX_ACTIVE_POSTS)
				break;
			if (post.getChattingUserCount() > 0)
				activePosts.add(post.getGuid());			
		}
		for (LivePost post : livePosts) {
			if (activePosts.size() >= MAX_ACTIVE_POSTS)
				break;
			if (post.getViewingUserCount() > 0)
				activePosts.add(post.getGuid());			
		}
		user.setActivePosts(activePosts);
	}
	
	private boolean checkUpdate(LiveUser user) {
		return user.getCacheAge() > 0; 
	}
	
	private void update(LiveUser user) {
		LiveState state = LiveState.getInstance();
		LiveUser newUser = (LiveUser) user.clone();
		List<PostView> recentPosts = getRecentPosts(user);
		initializeFromPosts(newUser, recentPosts); // FIXME - This is inefficient
		logger.debug("computing hotness for user {} old: {} new: " + newUser.getHotness().name(),
				user.getGuid(), user.getHotness().name());
		
		// Note that this doesn't notify the user if individual posts in the
		// list of active posts change their details, only if the *set* of active 
		// posts changes. Right now, we don't use the details from LivePost for
		// anything important in the client so this doesn't matter, but if we
		// start paying more attention to LivePost on the client this needs
		// to be fixed.
		if (!newUser.equals(user)) {
			state.updateLiveUser(newUser);
			// Remember to update sendAllNotifications if you add a new one here
			if (!newUser.getHotness().equals(user.getHotness()))
				msgSender.sendHotnessChanged(newUser);
			if (!newUser.getActivePosts().equals(user.getActivePosts()))
				msgSender.sendActivePostsChanged(newUser);
		}
	}

	public void handlePostViewed(Guid userGuid, LivePost post) {
		LiveState state = LiveState.getInstance();
		LiveUser user = state.peekLiveUser(userGuid);
		if (user == null || !checkUpdate(user))
			return;
		update(user);
	}
	
	public void handleGroupMembershipChanged(LiveUser luser) {
		LiveState state = LiveState.getInstance();
		LiveUser newUser = (LiveUser) luser.clone();		
		initializeGroups(newUser);
		state.updateLiveUser(newUser);		
	}
	
	public void handlePostCreated(LiveUser luser) {
		LiveState state = LiveState.getInstance();
		LiveUser newUser = (LiveUser) luser.clone();		
		initializePostCount(newUser);
		state.updateLiveUser(newUser);			
	}	

	public void periodicUpdate(LiveUser user) {
		if (!checkUpdate(user))
			return;		
		update(user);
	}

	public void sendAllNotifications(LiveUser luser) {
		// Remember to change the update method as well when adding
		// a new notification
		msgSender.sendHotnessChanged(luser);
		msgSender.sendActivePostsChanged(luser);		
	}
}