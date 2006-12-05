package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class MySpacePersonBlockView extends BlogLikeBlockView {
	
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
		return "MySpace Blog";
	}

	@Override
	protected String getSummaryHeading() {
		return "MySpace";
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.MYSPACE;
	}
}
