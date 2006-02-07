package com.dumbhippo.live;

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
	
	static final int RECENT_POSTS_MAX_HISTORY = 20;
	
	static final int RECENT_POSTS_SEC = 60 * 60;
	
	private double computeInitTemperature(LiveUser user) {
		double ret = 0.0;	
		User dbUser;
		try {
			dbUser = identitySpider.lookupGuid(User.class, user.getUserId());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		// Look for max of 3 unviewed posts
		List<PostView> lastPosts = postingBoard.getReceivedPosts(new Viewpoint(dbUser), dbUser, 0, RECENT_POSTS_MAX_HISTORY);
		for (PostView post : lastPosts) {
			if (ret >= 3.0)
				break;
			Date postDate = post.getPost().getPostDate();
			Date cur = new Date();
			long timeDiff = cur.getTime() - postDate.getTime();
			// Should probably push this into DB query
			if (timeDiff < (RECENT_POSTS_SEC * 1000) && !post.isViewerHasViewed()) {
				ret += 1.0;
			}
		}
		return ret;
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
		double score = computeInitTemperature(user);
		Hotness hotness = hotnessFromScore(user, score);
		user.setHotness(hotness);
	}
	
	public void updateHotness(LiveUser user) {
		LiveState state = LiveState.getInstance();
		if (user.getCacheAge() > 0) {
			LiveUser newUser = (LiveUser) user.clone();
			initialize(newUser); // FIXME - This is extremely inefficient
			logger.debug("computing hotness for user " + user.getUserId() 
					     + " old: " + user.getHotness().name() + " new: " + newUser.getHotness().name());			
			if (newUser.getHotness() != user.getHotness()) {
				state.updateLiveUser(newUser);
				User dbUser;
				try {
					dbUser = identitySpider.lookupGuid(User.class, user.getUserId());
				} catch (NotFoundException e) {
					throw new RuntimeException(e);
				}
				msgSender.sendHotnessChanged(dbUser, newUser.getHotness());
			}
		} else {
			// If the user hasn't aged it was updated recently, wait for an age
			logger.debug("delaying hotness update for user " + user.getUserId());			
		}
	}

	public void handlePostViewed(Guid userGuid, LivePost post) {
		LiveState state = LiveState.getInstance();
		LiveUser user = state.peekLiveUser(userGuid);
		if (user == null)
			return;
		updateHotness(user);
	}

	public void periodicUpdate(LiveUser user) {
		updateHotness(user);
	}
}
