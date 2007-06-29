package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class DeliciousBlockView extends AbstractFeedEntryBlockView {
	
	public DeliciousBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public DeliciousBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	@Override
	protected String getElementName() {
		return "deliciousBoookmark";
	}
	
	@Override
	public String getIcon() {
		return "/images3/favicon_delicious.png";
		//return entry.getFeed().getFavicon();
	}

	@Override
	public String getTypeTitle() {
		return "del.icio.us bookmark";
	}

	public @Override String getSummaryHeading() {
		return "Shared";
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.DELICIOUS;
	}
}
