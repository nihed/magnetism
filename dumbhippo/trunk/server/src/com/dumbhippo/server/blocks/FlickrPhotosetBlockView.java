package com.dumbhippo.server.blocks;

import com.dumbhippo.Thumbnails;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.services.FlickrPhotosetView;

public class FlickrPhotosetBlockView extends AbstractPersonBlockView
	implements ExternalAccountBlockView, ThumbnailsBlockView, SimpleTitleBlockView {

	private FlickrPhotosetView photosetView;
	private String flickrOwnerId;
	
	public FlickrPhotosetBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
	}

	@Override
	public String getIcon() {
		return "/images3/" + ExternalAccountType.FLICKR.getIconName();
	}

	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement("flickrPhotoset",
				"userId", getUserView().getUser().getId(),
				"title", photosetView.getTitle());
		writeThumbnailsToXmlBuilder(builder, this);
		builder.closeElement();
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.FLICKR;
	}

	void populate(PersonView userView, FlickrPhotosetView photosetView, String flickrOwnerId) {
		super.partiallyPopulate(userView);
		this.photosetView = photosetView;
		this.flickrOwnerId = flickrOwnerId;
		this.setPopulated(true);
	}
	
	public Thumbnails getThumbnails() {
		return photosetView.getThumbnails();
	}

	public String getMoreThumbnailsLink() {
		return photosetView.getUrl(flickrOwnerId);
	}

	public String getMoreThumbnailsTitle() {
		return "All photos in '" + photosetView.getTitle() + "'";
	}

	public String getTitle() {
		return photosetView.getTitle();
	}

	public String getLink() {
		return getMoreThumbnailsLink();
	}

	@Override
	public String getTypeTitle() {
		return "Flickr photoset";
	}

	public String getTitleForHome() {
		return getTitle();
	}
}
