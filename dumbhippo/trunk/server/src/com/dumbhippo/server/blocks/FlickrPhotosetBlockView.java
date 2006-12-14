package com.dumbhippo.server.blocks;

import com.dumbhippo.BasicThumbnails;
import com.dumbhippo.Thumbnail;
import com.dumbhippo.Thumbnails;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.services.FlickrPhotosetView;

public class FlickrPhotosetBlockView extends AbstractPersonBlockView
	implements ExternalAccountBlockView, ThumbnailsBlockView, TitleBlockView {

	private FlickrPhotosetView photosetView;
	private String flickrOwnerId;
	
	public FlickrPhotosetBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}

	public FlickrPhotosetBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
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
		Thumbnails thumbnails = photosetView.getThumbnails();
		if (thumbnails == null) {
			thumbnails = new BasicThumbnails(TypeUtils.emptyList(Thumbnail.class), 0, 50, 50);
		}
		return thumbnails;
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

	public @Override String getSummaryHeading() {
		return "Posted Flickr photoset";
	}

	public @Override String getSummaryLink() {
		return getMoreThumbnailsLink();
	}

	public @Override String getSummaryLinkText() {
		return getMoreThumbnailsTitle();
	}
}
