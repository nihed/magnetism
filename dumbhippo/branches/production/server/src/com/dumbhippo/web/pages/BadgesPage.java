package com.dumbhippo.web.pages;

import java.util.Arrays;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.tags.FlashBadge;

public class BadgesPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(BadgesPage.class);	

	private ListBean<FlashBadge> badges;
	
	public ListBean<FlashBadge> getBadges() {
		if (badges == null)
			badges = new ListBean<FlashBadge>(Arrays.asList(FlashBadge.values()));
		return badges;
	}
	
	public int getMaxBadgeHeight() {
		int maxHeight = 0;
		for (FlashBadge badge : FlashBadge.values()) {
			if (badge.getHeight() > maxHeight)
				maxHeight = badge.getHeight();
		}
		return maxHeight;
	}
}
