package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class PicasaPersonBlockView extends ExternalThumbnailedPersonBlockView {

	public PicasaPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public PicasaPersonBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	@Override
	public ExternalAccountType getAccountType() {
		return ExternalAccountType.PICASA;
	}

	@Override
	public String getTitle() {
		return "New albums posted";
	}

	@Override
	public String getTypeTitle() {
		return "Picasa albums";
	}

	@Override
	protected String getElementName() {
		return "picasaPerson";
	}

	public @Override String getSummaryHeading() {
		return "Posted albums";
	}

	public @Override String getSummaryLink() {
		return getMoreThumbnailsLink();
	}

	public @Override String getSummaryLinkText() {
		return getMoreThumbnailsTitle();
	}
}
