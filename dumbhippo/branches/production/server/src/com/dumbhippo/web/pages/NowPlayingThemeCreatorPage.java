package com.dumbhippo.web.pages;

import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.web.WebEJBUtil;

public class NowPlayingThemeCreatorPage extends AbstractSigninRequiredPage {

	private MusicSystem musicSystem;
	private NowPlayingTheme theme;
	
	public NowPlayingThemeCreatorPage() {
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
	}	
	
	public void setThemeId(String id) {
		if (theme != null)
			throw new IllegalStateException("set the theme twice");
		if (id == null || id.trim().length() == 0)
			return;
		try {
			theme = musicSystem.lookupNowPlayingTheme(id);
		} catch (ParseException e) {
			theme = null;
		} catch (NotFoundException e) {
			theme = null;
		}
	}
	
	public NowPlayingTheme getTheme() {	
		return theme;
	}
	
	public String getThemeId() {
		 return theme.getId();
	}
}
