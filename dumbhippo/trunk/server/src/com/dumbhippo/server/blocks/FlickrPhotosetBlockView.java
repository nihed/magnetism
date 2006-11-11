package com.dumbhippo.server.blocks;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class FlickrPhotosetBlockView extends AbstractPersonBlockView
	implements ExternalAccountBlockView {

	public FlickrPhotosetBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
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
}
