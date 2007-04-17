package com.dumbhippo.server.blocks;

import java.util.Collection;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.YouTubeUpdater;
import com.dumbhippo.services.YouTubeVideo;

@Stateless
public class YouTubeBlockHandlerBean extends
		AbstractExternalThumbnailedPersonBlockHandlerBean<YouTubePersonBlockView> implements
		YouTubeBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(YouTubeBlockHandlerBean.class);	

	@EJB
	private YouTubeUpdater youTubeUpdater;
	
	protected YouTubeBlockHandlerBean() {
		super(YouTubePersonBlockView.class, ExternalAccountType.YOUTUBE, BlockType.YOUTUBE_PERSON);
	}

	public void onYouTubeRecentVideosChanged(String username, List<? extends YouTubeVideo> videos) {
		logger.debug("most recent YouTube videos changed for " + username);

		if (videos.size() == 0) {
			logger.debug("not restacking youtube block since videos count is 0");
			return;
		}
		
		long now = System.currentTimeMillis();
		Collection<User> users = youTubeUpdater.getAccountLovers(username);
		for (User user : users) {
			stacker.stack(getKey(user), now, user, false, StackReason.BLOCK_UPDATE);
		}
	}
}
