package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class DiggBlockView extends AbstractFeedEntryBlockView {
	
	public DiggBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public DiggBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	@Override
	protected String getElementName() {
		return "diggDuggEntry";
	}
	
	@Override
	public String getIcon() {
		return "/images3/favicon_digg.png";
		//return entry.getFeed().getFavicon();
	}

	@Override
	public String getTypeTitle() {
		return "Dugg story";
	}

	public @Override String getSummaryHeading() {
		return "Dugg";
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.DIGG;
	}
}
