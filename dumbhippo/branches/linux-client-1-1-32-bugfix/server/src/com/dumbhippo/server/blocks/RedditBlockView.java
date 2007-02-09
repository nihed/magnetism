package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class RedditBlockView extends AbstractFeedEntryBlockView {
	
	public RedditBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public RedditBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	@Override
	protected String getElementName() {
		return "redditActivityEntry";
	}
	
	@Override
	public String getIcon() {
		return "/images3/favicon_reddit.png";
		//return entry.getFeed().getFavicon();
	}

	@Override
	public String getTypeTitle() {
		return "Reddit comment or post";
	}

	public @Override String getSummaryHeading() {
		return "Reddit";
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.REDDIT;
	}
}
