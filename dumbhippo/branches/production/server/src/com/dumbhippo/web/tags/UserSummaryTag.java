package com.dumbhippo.web.tags;

import java.io.IOException;

public class UserSummaryTag extends AbstractFlashTag {
	private FlashBadge badge;
	
	public void setHeight(int height) {		
		switch (height) {
		case 74:
			badge = FlashBadge.USER_SUMMARY_250_74;
			break;					
		case 180:
			badge = FlashBadge.USER_SUMMARY_250_180;
			break;
		case 255:
			badge = FlashBadge.USER_SUMMARY_250_255;
			break;
		}
		if (badge == null)
			throw new IllegalStateException("height not supported for userSummary badge " + height);
	}
	
	@Override
	public void doTag() throws IOException {
		if (badge == null)
			throw new IllegalStateException("must specify a height for userSummary");
		
		doTag(badge, "dh-badge-user-summary", null);
	}
}
