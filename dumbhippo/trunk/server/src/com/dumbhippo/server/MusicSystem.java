package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.User;

@Local
public interface MusicSystem {

	public NowPlayingTheme getCurrentNowPlayingTheme(User user) throws NotFoundException;
	public void setCurrentNowPlayingTheme(UserViewpoint viewpoint, User user, NowPlayingTheme theme);
	
	public NowPlayingThemesBundle getNowPlayingThemesBundle(Viewpoint viewpoint, User user);
	
	public NowPlayingTheme createNewNowPlayingTheme(UserViewpoint viewpoint, NowPlayingTheme basedOn);
	
	public NowPlayingTheme lookupNowPlayingTheme(String id) throws ParseException, NotFoundException;
	public NowPlayingTheme lookupNowPlayingTheme(Guid id) throws NotFoundException; 
	
	public void setNowPlayingThemeImage(UserViewpoint viewpoint, String id, String type, String shaSum) throws NotFoundException, ParseException;
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException;

	public List<TrackView> getPopularTrackViews(int maxResults) throws NotFoundException;
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults) throws NotFoundException;
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, User user, int maxResults) throws NotFoundException;
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, Group group, int maxResults) throws NotFoundException;
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, Group group, int maxResults) throws NotFoundException;
	
	public TrackView songSearch(Viewpoint viewpoint, String artist, String album, String name) throws NotFoundException;
	
	public AlbumView albumSearch(Viewpoint viewpoint, String artist, String album) throws NotFoundException;
	
	public ArtistView artistSearch(Viewpoint viewpoint, String artist) throws NotFoundException;
	
	public List<PersonMusicView> getRelatedPeopleWithTracks(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<PersonMusicView> getRelatedPeopleWithAlbums(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<PersonMusicView> getRelatedPeopleWithArtists(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<AlbumView> getLatestAlbumViews(Viewpoint viewpoint, User user, int maxResults) throws NotFoundException;
	
	public List<ArtistView> getLatestArtistViews(Viewpoint viewpoint, User user, int maxResults) throws NotFoundException;
}
