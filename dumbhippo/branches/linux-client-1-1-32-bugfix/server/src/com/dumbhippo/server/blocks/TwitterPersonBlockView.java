package com.dumbhippo.server.blocks;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class TwitterPersonBlockView extends AbstractFeedEntryBlockView {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(TwitterPersonBlockView.class);	
	
	public TwitterPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);		
	}
	
	public TwitterPersonBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	@Override
	protected String getElementName() {
		return "twitterPerson";
	}
	
	@Override
	public String getIcon() {
		return "/images3/" + getAccountType().getIconName();
	}

	@Override
	public String getTypeTitle() {
		return "Twitter Status";
	}

	@Override 
	public String getSummaryHeading() {
		return "Twitter";
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.TWITTER;
	}
}
