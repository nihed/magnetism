package com.dumbhippo.live;

import java.util.List;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Viewpoint;

// Implementation of LivePostUpdater
@Stateless
public class LivePostUpdaterBean implements LivePostUpdater {
	@EJB
	PostingBoard postingBoard;
	
	public void initialize(LivePost livePost) {
		List<PersonPostData> viewers = postingBoard.getPostViewers(null, livePost.getPostId(), LivePost.MAX_STORED_VIEWERS);
		for (PersonPostData viewerData : viewers) {
			livePost.addViewer(viewerData.getPerson().getGuid(), viewerData.getClickedDate());
		}
		PostView pv;
		try {
			pv = postingBoard.loadPost(new Viewpoint(null), livePost.getPostId());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		Post postObj = pv.getPost();
		List<PostMessage> messages = postingBoard.getRecentPostMessages(postObj, 60);
		livePost.setRecentMessageCount(messages.size());
	}
}
