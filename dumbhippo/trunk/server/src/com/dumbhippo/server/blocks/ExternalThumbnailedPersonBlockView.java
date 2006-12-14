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
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

public abstract class ExternalThumbnailedPersonBlockView extends AbstractPersonBlockView
	implements ExternalAccountBlockView, ThumbnailsBlockView, TitleBlockView {

	protected ExternalAccountView externalAccountView;
	
	public ExternalThumbnailedPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public ExternalThumbnailedPersonBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}

	protected abstract String getElementName();
	
	@Override
	public String getIcon() {
		return "/images3/" + getAccountType().getIconName();
	}

	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement(getElementName(),
							"userId", getUserView().getUser().getId());
		writeThumbnailsToXmlBuilder(builder, this);
		builder.closeElement();		
	}

	void populate(PersonView userView, ExternalAccountView externalAccountView) {
		super.partiallyPopulate(userView);
		this.externalAccountView = externalAccountView;
		setPopulated(true);
	}
	
	public abstract ExternalAccountType getAccountType();

	public Thumbnails getThumbnails() {
		Thumbnails thumbnails = externalAccountView.getThumbnails();
		if (thumbnails == null) {
			thumbnails = new BasicThumbnails(TypeUtils.emptyList(Thumbnail.class), 0, 50, 50);
		}
		return thumbnails;
	}

	public String getMoreThumbnailsLink() {
		return externalAccountView.getLink();
	}

	public String getMoreThumbnailsTitle() {
		return externalAccountView.getExternalAccount().getLinkText();
	}

	public String getTitleForHome() {
		return getTitle();
	}
	
	public abstract String getTitle();

	public String getLink() {
		return getMoreThumbnailsLink(); 
	}

	@Override
	public abstract String getTypeTitle();
}
