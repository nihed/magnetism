package com.dumbhippo.web.pages;

import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.NowPlayingThemeSystem;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.web.PagePositions;
import com.dumbhippo.web.PagePositionsBean;
import com.dumbhippo.web.WebEJBUtil;

public class NowPlayingThemesPage extends AbstractSigninOptionalPage {

	@PagePositions
	private PagePositionsBean pagePositions;
	
	private NowPlayingThemeSystem nowPlayingSystem;
	
	private NowPlayingTheme currentTheme;
	private Pageable<NowPlayingTheme> myThemes;	
	private Pageable<NowPlayingTheme> friendsThemes;
	private Pageable<NowPlayingTheme> randomThemes;
	
	public NowPlayingThemesPage() {
		nowPlayingSystem = WebEJBUtil.defaultLookup(NowPlayingThemeSystem.class);
	}

	private void load() {
		Viewpoint viewpoint = getSignin().getViewpoint();
				
		if (randomThemes == null) {
			randomThemes = pagePositions.createPageable("randomThemes");
			nowPlayingSystem.getAllThemes(viewpoint, randomThemes);
		}
		if (currentTheme == null && getSignin().isValid()) {
			User user = getUserSignin().getUser();	
			currentTheme = nowPlayingSystem.getCurrentTheme(viewpoint, user);		
			myThemes = pagePositions.createPageable("myThemes");
			nowPlayingSystem.getMyThemes(viewpoint, user, myThemes);
			friendsThemes = pagePositions.createPageable("friendsThemes");
			nowPlayingSystem.getFriendsThemes(viewpoint, user, friendsThemes);			
		}
	}
	
	public NowPlayingTheme getCurrentTheme() {
		load();
		return currentTheme;
	}
	
	public Pageable<NowPlayingTheme> getFriendsThemes() {
		load();
		return friendsThemes;
	}
	
	public Pageable<NowPlayingTheme> getMyThemes() {
		load();
		return myThemes;
	}
	
	public Pageable<NowPlayingTheme> getRandomThemes() {
		load();
		return randomThemes;
	}
	
	public String getRadarCharacterId() {
		if (getSignin().isValid()) {
			return getUserSignin().getUserId();
		}
		return identitySpider.getCharacter(Character.MUGSHOT).getId();
	}
}
