package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.User;

@Local
public interface NowPlayingThemeSystem {

	public NowPlayingTheme getCurrentNowPlayingTheme(User user) throws NotFoundException;
	public void setCurrentNowPlayingTheme(UserViewpoint viewpoint, User user, NowPlayingTheme theme);
	
	public NowPlayingTheme getCurrentTheme(Viewpoint viewpoint, User user);
	public void getFriendsThemes(Viewpoint viewpoint, User user, Pageable<NowPlayingTheme> pageable);
	public void getMyThemes(Viewpoint viewpoint, User user, Pageable<NowPlayingTheme> pageable);
	public void getAllThemes(Viewpoint viewpoint, Pageable<NowPlayingTheme> pageable);	
	
	public List<NowPlayingTheme> getExampleNowPlayingThemes(Viewpoint viewpoint, int maxResults);
	
	public NowPlayingTheme createNewNowPlayingTheme(UserViewpoint viewpoint, NowPlayingTheme basedOn);
	
	public NowPlayingTheme lookupNowPlayingTheme(String id) throws ParseException, NotFoundException;
	public NowPlayingTheme lookupNowPlayingTheme(Guid id) throws NotFoundException; 
	
	public void setNowPlayingThemeImage(UserViewpoint viewpoint, String id, String type, String shaSum) throws NotFoundException, ParseException;

}
