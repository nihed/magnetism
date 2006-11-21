package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class YouTubePersonBlockView extends ExternalThumbnailedPersonBlockView {

	public YouTubePersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public YouTubePersonBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	@Override
	public ExternalAccountType getAccountType() {
		return ExternalAccountType.YOUTUBE;
	}

	@Override
	public String getTitle() {
		return "New videos posted";
	}

	@Override
	public String getTypeTitle() {
		return "YouTube videos";
	}

	@Override
	protected String getElementName() {
		return "youTubePerson";
	}
}
