package com.dumbhippo.server;

import java.util.Date;
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
	
	public List<NowPlayingTheme> getExampleNowPlayingThemes(Viewpoint viewpoint, int maxResults);
	
	public NowPlayingTheme createNewNowPlayingTheme(UserViewpoint viewpoint, NowPlayingTheme basedOn);
	
	public NowPlayingTheme lookupNowPlayingTheme(String id) throws ParseException, NotFoundException;
	public NowPlayingTheme lookupNowPlayingTheme(Guid id) throws NotFoundException; 
	
	public void setNowPlayingThemeImage(UserViewpoint viewpoint, String id, String type, String shaSum) throws NotFoundException, ParseException;
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException;

	public List<TrackView> getPopularTrackViews(int maxResults) ;
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults);
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, User user, int maxResults);
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, Group group, int maxResults);
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, Group group, int maxResults);

	/**
	 * Returns the globally most played tracks in the system.
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param maxResults maximum number of results to return, if positive 
	 * @return a list of TrackViews for the most popular tracks
	 */
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, int maxResults);
	
	/**
	 * Returns the globally most played tracks in the system since a specified
	 * date/time. (Since we only store one date per track per user, unlike
	 * getFrequentTrackViews, "most played" is computed not as number of total
	 * plays, but rather as number of distinct users playing the track)
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param since only consider tracks played after this date.
	 * @param maxResults maximum number of results to return, if positive 
	 * @return a list of TrackViews for the most popular tracks
	 */
	public List<TrackView> getFrequentTrackViewsSince(Viewpoint viewpoint, Date since, int maxResults);
	
	/**
	 * Returns the most recent tracks in the system that have only been
	 * played once, ever.
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param maxResults maximum number of results to return, if positive
	 * @return a list of TrackViews for the most recent songs played only once
	 */
	public List<TrackView> getOnePlayTrackViews(Viewpoint viewpoint, int maxResults);

	/**
	 * Returns a list of songs played most recently by friends of the viewing user.
	 * (This could be extended easily to handle friends of a different user, but
	 * we don't need that at the moment.)
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param maxResults maximum number of results to return, if positive
	 * @return a list of TrackViews for the most recent songs played by the viewpoints contacts
	 */
	public List<TrackView> getFriendsLatestTrackViews(UserViewpoint viewpoint, int maxResults);
	
	public TrackView songSearch(Viewpoint viewpoint, String artist, String album, String name) throws NotFoundException;
	
	public AlbumView albumSearch(Viewpoint viewpoint, String artist, String album) throws NotFoundException;
	
	public ArtistView artistSearch(Viewpoint viewpoint, String artist) throws NotFoundException;
	
	public List<PersonMusicView> getRelatedPeopleWithTracks(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<PersonMusicView> getRelatedPeopleWithAlbums(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<PersonMusicView> getRelatedPeopleWithArtists(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<AlbumView> getLatestAlbumViews(Viewpoint viewpoint, User user, int maxResults);
	
	public List<ArtistView> getLatestArtistViews(Viewpoint viewpoint, User user, int maxResults);
}
