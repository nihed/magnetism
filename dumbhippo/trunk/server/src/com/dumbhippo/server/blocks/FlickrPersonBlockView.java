package com.dumbhippo.server.blocks;

import com.dumbhippo.Thumbnails;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.Viewpoint;

public class FlickrPersonBlockView extends AbstractPersonBlockView
	implements ExternalAccountBlockView, ThumbnailsBlockView, SimpleTitleBlockView {

	private ExternalAccountView externalAccountView;
	
	public FlickrPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
	}

	@Override
	public String getIcon() {
		return ExternalAccountType.FLICKR.getIconName();
	}

	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		// FIXME
	}

	void populate(ExternalAccountView externalAccountView) {
		this.externalAccountView = externalAccountView;
		setPopulated(true);
	}
	
	public ExternalAccountType getAccountType() {
		return ExternalAccountType.FLICKR;
	}

	public Thumbnails getThumbnails() {
		return externalAccountView.getThumbnails();
	}

	public String getMoreThumbnailsLink() {
		return externalAccountView.getLink();
	}

	public String getMoreThumbnailsTitle() {
		return externalAccountView.getExternalAccount().getLink();
	}

	public String getTitleForHome() {
		return getTitle();
	}
	
	public String getTitle() {
		return "New photos posted";
	}

	public String getLink() {
		return "FIXME"; 
	}

	@Override
	public String getTypeTitle() {
		return "Flickr photos";
	}
}
