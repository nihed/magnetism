package com.dumbhippo.web;

import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NowPlayingThemesBundle;

public class NowPlayingThemesPage extends AbstractSigninRequiredPage {

	private MusicSystem musicSystem;
	
	private NowPlayingTheme currentTheme;
	private ListBean<NowPlayingTheme> myThemes;
	private ListBean<NowPlayingTheme> friendsThemes;
	private ListBean<NowPlayingTheme> randomThemes;
	private boolean gotBundle;
	
	public NowPlayingThemesPage() {
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
		gotBundle = false;
	}

	private void checkBundle() {
		if (!gotBundle) {
			NowPlayingThemesBundle bundle = musicSystem.getNowPlayingThemesBundle(getSignin().getViewpoint(), getUserSignin().getUser());
			
			currentTheme = bundle.getCurrentTheme();
			myThemes = new ListBean<NowPlayingTheme>(bundle.getMyThemes());
			friendsThemes = new ListBean<NowPlayingTheme>(bundle.getFriendsThemes());
			randomThemes = new ListBean<NowPlayingTheme>(bundle.getRandomThemes());
			
			gotBundle = true;
		}
	}
	
	public NowPlayingTheme getCurrentTheme() {
		checkBundle();
		return currentTheme;
	}
	
	public ListBean<NowPlayingTheme> getFriendsThemes() {
		checkBundle();
		return friendsThemes;
	}
	
	public ListBean<NowPlayingTheme> getMyThemes() {
		checkBundle();
		return myThemes;
	}
	
	public ListBean<NowPlayingTheme> getRandomThemes() {
		checkBundle();
		return randomThemes;
	}
}
