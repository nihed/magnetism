package com.dumbhippo.web.tags;

import java.io.IOException;

public class FlashBadgeTag extends AbstractFlashTag {

	private FlashBadge badge;

	public void setBadge(FlashBadge badge) {
		this.badge = badge;
	}
	
	
	@Override
	public void doTag() throws IOException {
		doTag(badge, "dh-badge", null, null);
	}
}
