package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class FlickrPersonBlockView extends ExternalThumbnailedPersonBlockView {

	public FlickrPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}

	public FlickrPersonBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}

	@Override
	public ExternalAccountType getAccountType() {
		return ExternalAccountType.FLICKR;
	}
	
	@Override
	public String getTitle() {
		return "New photos posted";
	}

	@Override
	public String getTypeTitle() {
		return "Flickr photos";
	}

	@Override
	protected String getElementName() {
		return "flickrPerson";
	}

	public @Override String getSummaryHeading() {
		return getTitle();
	}

	public @Override String getSummaryLink() {
		return getMoreThumbnailsLink();
	}

	public @Override String getSummaryLinkText() {
		return getMoreThumbnailsTitle();
	}
}
