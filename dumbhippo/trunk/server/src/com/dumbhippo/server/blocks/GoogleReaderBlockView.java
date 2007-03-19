package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class GoogleReaderBlockView extends AbstractFeedEntryBlockView {
	
	public GoogleReaderBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public GoogleReaderBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	@Override
	protected String getElementName() {
		return "googleReaderPerson";
	}
	
	@Override
	public String getIcon() {
		return "/images3/favicon_google_reader.png";
		//return entry.getFeed().getFavicon();
	}

	@Override
	public String getTypeTitle() {
		return "Google Reader share";
	}

	public @Override String getSummaryHeading() {
		return "Shared";
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.GOOGLE_READER;
	}
}
