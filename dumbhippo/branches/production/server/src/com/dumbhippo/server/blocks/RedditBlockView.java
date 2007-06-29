package com.dumbhippo.server.blocks;

import com.dumbhippo.StringUtils;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class RedditBlockView extends AbstractFeedEntryBlockView {
	
	public static String DEFAULT_TYPE_TITLE = "Reddit comment or post";
	
	private String typeTitle;
	
	public RedditBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public RedditBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);	
	}
	
	@Override
	protected String getElementName() {
		return "redditActivityEntry";
	}
	
	@Override
	public String getIcon() {
		return "/images3/favicon_reddit.png";
		//return entry.getFeed().getFavicon();
	}

	@Override
	public String getTypeTitle() {
		// other type titles: "Liked on Reddit", "Disliked on Reddit"
		if (typeTitle == null)
		    return DEFAULT_TYPE_TITLE;
		else
			return typeTitle;
	}

	public void setTypeTitle(String typeTitle) {
		this.typeTitle = typeTitle;
	}
	
	public @Override String getSummaryHeading() {
		return "Reddit";
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.REDDIT;
	}
	
	@Override
	public String getDescription() {
		String description = StringUtils.ellipsizeText(getEntry().getDescription());
		// Reddit description is usually pretty useless, consisting of [link][more]
		if (description.equals("[link][more]"))
			return "";
		
		return description;
	}
}
