package com.dumbhippo.server;

import java.util.List;

import com.dumbhippo.persistence.NowPlayingTheme;

public final class NowPlayingThemesBundle {

	private NowPlayingTheme currentTheme;
	private List<NowPlayingTheme> myThemes;
	private List<NowPlayingTheme> friendsThemes;
	private List<NowPlayingTheme> randomThemes;
	
	public NowPlayingThemesBundle() {
		
	}
	
	public NowPlayingTheme getCurrentTheme() {
		return currentTheme;
	}
	public void setCurrentTheme(NowPlayingTheme currentTheme) {
		this.currentTheme = currentTheme;
	}
	public List<NowPlayingTheme> getFriendsThemes() {
		return friendsThemes;
	}
	public void setFriendsThemes(List<NowPlayingTheme> friendsThemes) {
		this.friendsThemes = friendsThemes;
	}
	public List<NowPlayingTheme> getMyThemes() {
		return myThemes;
	}
	public void setMyThemes(List<NowPlayingTheme> myThemes) {
		this.myThemes = myThemes;
	}
	public List<NowPlayingTheme> getRandomThemes() {
		return randomThemes;
	}
	public void setRandomThemes(List<NowPlayingTheme> randomThemes) {
		this.randomThemes = randomThemes;
	}
}
