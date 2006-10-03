package com.dumbhippo.live;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.UserViewpoint;

// Implementation of LiveClientDataUpdater
@Stateless
public class LiveClientDataUpdaterBean implements LiveClientDataUpdater {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveClientDataUpdaterBean.class);
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PostingBoard postingBoard;
	
	@EJB
	private MessageSender msgSender;
	
	private static final int MAX_ACTIVE_POSTS = 3;
	
	private static final int RECENT_POSTS_MAX_HISTORY = 20;
	
	private static final int CURRENT_POSTS_SEC = 60 * 60;
	
	private boolean postIsCurrent(PostView post) {
		Date postDate = post.getPost().getPostDate();
		Date cur = new Date();
		long timeDiff = cur.getTime() - postDate.getTime();
		// Should probably push this into DB query
		return timeDiff < (CURRENT_POSTS_SEC * 1000);		
	}
	
	private List<PostView> getRecentPosts(LiveClientData clientData) {
		User dbUser = identitySpider.lookupUser(clientData.getGuid());
		UserViewpoint viewpoint = new UserViewpoint(dbUser);
		// FIXME this filters out feed posts now, I don't think that's the intention here
		List<PostView> posts = postingBoard.getReceivedPosts(viewpoint, dbUser, 0, RECENT_POSTS_MAX_HISTORY);
		logger.debug("Got {} for getReceivedPosts for user {}", posts.size(), clientData.getGuid());
		return posts;
	}

	private double computeInitialTemperature(LiveClientData clientData, List<PostView> posts) {
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
	private Hotness hotnessFromScore(LiveClientData clientData, double score) {
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
	
	public void initialize(LiveClientData user) {
		initializeFromPosts(user, getRecentPosts(user));
	}
	
	private void initializeFromPosts(LiveClientData clientData, List<PostView> recentPosts) {
		LiveState state = LiveState.getInstance();		
		double score = computeInitialTemperature(clientData, recentPosts);
		Hotness hotness = hotnessFromScore(clientData, score);
		clientData.setHotness(hotness);
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
		clientData.setActivePosts(activePosts);
	}
	
	public void periodicUpdate(Guid userGuid) {
		LiveState state = LiveState.getInstance();
		LiveClientData clientData = state.peekLiveClientDataForUpdate(userGuid);
		if (clientData == null) // expired from the cache since we listed all GUIDs
			return;
		
		LiveClientData newData = (LiveClientData) clientData.clone();
		List<PostView> recentPosts = getRecentPosts(clientData);
		initializeFromPosts(newData, recentPosts); // FIXME - This is inefficient
		logger.debug("computing hotness for user {} old: {} new: " + newData.getHotness().name(),
				clientData.getGuid(), clientData.getHotness().name());
		
		state.updateLiveClientData(newData);

		// Note that this doesn't notify the user if individual posts in the
		// list of active posts change their details, only if the *set* of active 
		// posts changes. Right now, we don't use the details from LivePost for
		// anything important in the client so this doesn't matter, but if we
		// start paying more attention to LivePost on the client this needs
		// to be fixed.
		if (!newData.equals(clientData)) {
			// Remember to update sendAllNotifications if you add a new one here
			if (!newData.getHotness().equals(clientData.getHotness()))
				msgSender.sendHotnessChanged(newData);
			if (!newData.getActivePosts().equals(clientData.getActivePosts()))
				msgSender.sendActivePostsChanged(newData);
		}
	}

	public void sendAllNotifications(LiveClientData luser) {
		// Remember to change the update method as well when adding
		// a new notification
		msgSender.sendHotnessChanged(luser);
		msgSender.sendActivePostsChanged(luser);		
	}
}