package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class BlogBlockView extends AbstractSingleBlockForFeedBlockView {
	
	public BlogBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public BlogBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	@Override
	protected String getElementName() {
		return "blogPerson";
	}
	
	@Override
	public String getIcon() {
		return "/images3/blog_icon.png";
		//return entry.getFeed().getFavicon();
	}

	@Override
	public String getTypeTitle() {
		return "Blog post";
	}

	public @Override String getSummaryHeading() {
		return "Blogged";
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.BLOG;
	}
}
