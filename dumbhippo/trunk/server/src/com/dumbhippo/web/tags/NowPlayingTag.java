package com.dumbhippo.web.tags;

import java.io.IOException;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;

public class NowPlayingTag extends AbstractFlashTag {

	static final private String LABEL = "<b>What I'm listening to right now</b>\n";	
	
	private String themeId;	
	private String forceMode;
	
	public void setThemeId(String themeId) {
		try {
			// paranoia since we output this again and it could be provided
			// by a user
			Guid.validate(themeId);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		this.themeId = themeId;	
	}

	public void setForceMode(String forceMode) {
		this.forceMode = forceMode;
	}
	
	@Override
	public void doTag() throws IOException {
		doTag(FlashBadge.NOW_PLAYING_440_120, "dh-nowplaying", null, LABEL, 
				"themeId", themeId, "forceMode", forceMode);
	}
}
