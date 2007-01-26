package com.dumbhippo.server.blocks;

import com.dumbhippo.StringUtils;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class NetflixBlockView extends AbstractFeedEntryBlockView {
	
	public NetflixBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public NetflixBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	@Override
	protected String getElementName() {
		return "netflixMovie";
	}
	
	@Override
	public String getIcon() {
		return "/images3/favicon_netflix.png";
	}

	@Override
	public String getTypeTitle() {
		return "Netflix movie";
	}

	public @Override String getSummaryHeading() {
		return "Rented";
	}

	public ExternalAccountType getAccountType() {
		return ExternalAccountType.NETFLIX;
	}
	
	@Override
	public String getDescription() {
		String description = getEntry().getDescription();
		// We want to parse out "Shipped on mm/dd/yy."
		String shippedStr = "Shipped on ";
		int i = description.indexOf(shippedStr); 
		if (i >= 0) {
			int j = description.indexOf(".", i + shippedStr.length()) + 1;
			if (j > 0 && j < description.length()) {
			    String newDescription = description.substring(j).trim();
			 	return newDescription;
			}
		}
		return description;
	}
	
	public String getImageUrl() {
		String movieId = StringUtils.findParamValueInUrl(getLink(), "movieid");		
		return "http://cdn.nflximg.com/us/boxshots/small/" + movieId + ".jpg";
	}
}