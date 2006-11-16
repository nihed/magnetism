package com.dumbhippo.server.blocks;

import com.dumbhippo.Thumbnails;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

public class YouTubePersonBlockView extends AbstractPersonBlockView
	implements ExternalAccountBlockView, ThumbnailsBlockView, SimpleTitleBlockView {

	private ExternalAccountView externalAccountView;
	
	public YouTubePersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public YouTubePersonBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}

	@Override
	public String getIcon() {
		return "/images3/" + ExternalAccountType.YOUTUBE.getIconName();
	}

	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement("youTubePerson",
							"userId", getUserView().getUser().getId());
		writeThumbnailsToXmlBuilder(builder, this);
		builder.closeElement();		
	}

	void populate(PersonView userView, ExternalAccountView externalAccountView) {
		super.partiallyPopulate(userView);
		this.externalAccountView = externalAccountView;
		setPopulated(true);
	}
	
	public ExternalAccountType getAccountType() {
		return ExternalAccountType.YOUTUBE;
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
		return "New videos posted";
	}

	public String getLink() {
		return getMoreThumbnailsLink(); 
	}

	@Override
	public String getTypeTitle() {
		return "YouTube videos";
	}
}
