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
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Viewpoint;

// Implementation of LiveUserUpdater
@Stateless
public class LiveUserUpdaterBean implements LiveUserUpdater {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveUserUpdaterBean.class);
	
	@EJB
	IdentitySpider identitySpider;
	
	@EJB
	PostingBoard postingBoard;
	
	@PersistenceContext(unitName = "dumbhippo")
	EntityManager em;
	
	@EJB
	MessageSender msgSender;
	
	static final int MAX_ACTIVE_POSTS = 3;
	
	static final int RECENT_POSTS_MAX_HISTORY = 20;
	
	static final int RECENT_POSTS_SEC = 60 * 60;
	
	private List<PostView> getRecentReceivedPosts(LiveUser user) {
		User dbUser;
		try {
			dbUser = identitySpider.lookupGuid(User.class, user.getGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}		
		List<PostView> lastPosts = postingBoard.getReceivedPosts(new Viewpoint(dbUser), dbUser, 0, RECENT_POSTS_MAX_HISTORY);
		List<PostView> result = new ArrayList<PostView>();
		for (PostView post: lastPosts) {
			Date postDate = post.getPost().getPostDate();
			Date cur = new Date();
			long timeDiff = cur.getTime() - postDate.getTime();
			// Should probably push this into DB query
			if (timeDiff < (RECENT_POSTS_SEC * 1000)) {
				result.add(post);
			}			
		}
		return result;
	}
	
	private double computeInitialTemperature(LiveUser user, List<PostView> posts) {
		double score = 0.0;	
		for (PostView post : posts) {
			// Look for max of 3 unviewed posts			
			if (score >= 3.0)
				break;
			 if (!post.isViewerHasViewed())
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
	
	public void initialize(LiveUser user) {
		initializeFromPosts(user, getRecentReceivedPosts(user));
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
		// Now add in less interesting ones
		while (activePosts.size() < MAX_ACTIVE_POSTS 
				&& livePosts.size() > 0) {
			LivePost post = livePosts.get(0);
			if (!activePosts.contains(post.getGuid())) {
				activePosts.add(post.getGuid());
			}
			livePosts.remove(0);
		}
	}
	
	private boolean checkUpdate(LiveUser user) {
		return user.getCacheAge() > 0; 
	}
	
	private void update(LiveUser user) {
		LiveState state = LiveState.getInstance();
		LiveUser newUser = (LiveUser) user.clone();
		List<PostView> recentPosts = getRecentReceivedPosts(user);
		initializeFromPosts(newUser, recentPosts); // FIXME - This is inefficient
		logger.debug("computing hotness for user " + user.getGuid() 
				+ " old: " + user.getHotness().name() + " new: " + newUser.getHotness().name());		
		if (!newUser.equals(user)) {
			state.updateLiveUser(newUser);
			// Remember to update sendAllNotifications if you add a new one here
			if (!newUser.getHotness().equals(user.getHotness()))
				msgSender.sendHotnessChanged(user);
			if (!newUser.getActivePosts().equals(user.getActivePosts()))
				msgSender.sendActivePostsChanged(user);
		}
	}

	public void handlePostViewed(Guid userGuid, LivePost post) {
		LiveState state = LiveState.getInstance();
		LiveUser user = state.peekLiveUser(userGuid);
		if (user == null || !checkUpdate(user))
			return;
		update(user);
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