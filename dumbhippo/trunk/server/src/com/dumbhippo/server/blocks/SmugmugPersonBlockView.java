package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class SmugmugPersonBlockView extends ExternalThumbnailedPersonBlockView {

	public SmugmugPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public SmugmugPersonBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	@Override
	public ExternalAccountType getAccountType() {
		return ExternalAccountType.SMUGMUG;
	}

	@Override
	public String getTitle() {
		return "New albums posted";
	}

	@Override
	public String getTypeTitle() {
		return "Smugmug albums";
	}

	@Override
	protected String getElementName() {
		return "smugmugPerson";
	}

	public @Override String getBlockSummaryHeading() {
		return "Posted new albums";
	}

	public @Override String getBlockSummaryLink() {
		return getMoreThumbnailsLink();
	}

	public @Override String getSummaryLinkText() {
		return getMoreThumbnailsTitle();
	}
}
