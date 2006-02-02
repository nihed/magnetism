package com.dumbhippo.live;

import java.util.List;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.server.PostingBoard;

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
	}
}
