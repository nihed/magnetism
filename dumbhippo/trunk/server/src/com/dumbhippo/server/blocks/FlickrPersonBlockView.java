package com.dumbhippo.server.blocks;

import java.util.List;

import com.dumbhippo.Thumbnail;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class FlickrPersonBlockView extends AbstractPersonBlockView
	implements ExternalAccountBlockView, ThumbnailsBlockView {

	public FlickrPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
	}

	@Override
	public String getIcon() {
		// FIXME
		return "/images3/musicradar_icon.png";
	}

	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		// FIXME
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.FLICKR;
	}
	
	public List<Thumbnail> getThumbnails() {
		// FIXME
		return null;
	}

	public int getThumbnailCount() {
		// FIXME
		return 0;
	}

	public String getMoreThumbnailsLink() {
		// FIXME
		return null;
	}

	public String getMoreThumbnailsTitle() {
		// FIXME
		return null;
	}	
}
