package com.dumbhippo.live;

import java.util.List;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SystemViewpoint;

// Implementation of LivePostUpdater
@Stateless
public class LivePostUpdaterBean implements LivePostUpdater {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LivePostUpdaterBean.class);
	
	static public final int RECENT_MESSAGES_AGE_IN_SECONDS = 180;
	
	@EJB
	private PostingBoard postingBoard;

	public void initialize(LivePost livePost) {
		int totalViewers = postingBoard.getPostViewerCount(livePost.getGuid());
		livePost.setTotalViewerCount(totalViewers);
		List<PersonPostData> viewers = postingBoard.getPostViewers(null, livePost.getGuid(), LivePost.MAX_STORED_VIEWERS);
		for (PersonPostData viewerData : viewers) {
			livePost.addViewer(viewerData.getPerson().getGuid(), viewerData.getClickedDate());
		}
		PostView pv;
		try {
			pv = postingBoard.loadPost(SystemViewpoint.getInstance(), livePost.getGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		Post postObj = pv.getPost();
		livePost.setRecentMessageCount(postingBoard.getRecentPostMessageCount(postObj, RECENT_MESSAGES_AGE_IN_SECONDS));
		
		logger.debug("livePost {} initialized with {} total viewers " + livePost.getRecentMessageCount() + " recent messages",
				livePost.getGuid(), livePost.getTotalViewerCount());
	}
}
