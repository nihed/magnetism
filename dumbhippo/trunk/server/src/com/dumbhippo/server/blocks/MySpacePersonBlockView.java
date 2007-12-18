package com.dumbhippo.server.blocks;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class MySpacePersonBlockView extends AbstractFeedEntryBlockView {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(MySpacePersonBlockView.class);	
	
	public MySpacePersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);		
	}
	
	public MySpacePersonBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	@Override
	protected String getElementName() {
		return "mySpacePerson";
	}
	
	@Override
	public String getIcon() {
		return "/images3/" + getAccountType().getIconName();
	}

	@Override
	public String getTypeTitle() {
		return "MySpace blog post";
	}

	public @Override String getBlockSummaryHeading() {
		return "Blogged";
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.MYSPACE;
	}
}
