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
	
	/**
	 * Retrieve the set of tracks that were played globally on the system most recently
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param pageable object providing range to retrieve and in which to store results
	 */
	public void pageLatestTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable);

	public List<TrackView> getPopularTrackViews(int maxResults);
	
	/**
	 * Retrieve the tracks that a user has listened to most recently
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param user the user listening to tracks
	 * @param pageable object providing range to retrieve and in which to store results
	 */
	public void pageLatestTrackViews(Viewpoint viewpoint, User user, Pageable<TrackView> pageable);
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults);
	
	/**
	 * Retrieve the tracks a user has listened to most frequently.
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param user the user listening to tracks
	 * @param pageable object providing range to retrieve and in which to store results  
	 */
	public void pageFrequentTrackViews(Viewpoint viewpoint, User user, Pageable<TrackView> pageable);
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, User user, int maxResults);
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, Group group, int maxResults);
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, Group group, int maxResults);

	/**
	 * Retrieve the globally most played tracks in the system.
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param pageable object providing range to retrieve and in which to store results  
	 */
	public void pageFrequentTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable);
	
	/**
	 * Retrieve the globally most played tracks in the system since a specified
	 * date/time. (Since we only store one date per track per user, unlike
	 * getFrequentTrackViews, "most played" is computed not as number of total
	 * plays, but rather as number of distinct users playing the track)
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param since only consider tracks played after this date.
	 * @param pageable object providing range to retrieve and in which to store results  
	 */
	public void pageFrequentTrackViewsSince(Viewpoint viewpoint, Date since, Pageable<TrackView> pageable);
	
	/**
	 * Retrieve the most recent tracks in the system that have only been
	 * played once, ever.
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param pageable object providing range to retrieve and in which to store results  
	 */
	public void pageOnePlayTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable);

	/**
	 * Retrieve a list of songs played most recently by friends of the viewing user.
	 * (This could be extended easily to handle friends of a different user, but
	 * we don't need that at the moment.)
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param pageable object providing range to retrieve and in which to store results  
	 */
	public void pageFriendsLatestTrackViews(UserViewpoint viewpoint, Pageable<TrackView> pageable);
	
	public TrackView songSearch(Viewpoint viewpoint, String artist, String album, String name) throws NotFoundException;
	
	public AlbumView albumSearch(Viewpoint viewpoint, String artist, String album) throws NotFoundException;

	/**
	 * Returns an ArtistView that contains the artist's name if there is a matching track for the 
	 * artist locally. Throws a NotFoundException if there is no matching track for the artist 
	 * locally.
	 * 
	 * @param viewpoint
	 * @param artist
	 * @return an ArtistView that contains the artist's name
	 * @throws NotFoundException
	 */
	public ArtistView artistSearch(Viewpoint viewpoint, String artist) throws NotFoundException;
	
	/**
	 * Returns an ExpandedArtistView that includes albums made by the artist. First, attempts to
	 * find a track with a matching artist among existing tracks. Queries an outside web service 
	 * if a track with a matching artist is not found locally. Throws a NotFoundException if there
	 * is no matching artist.
	 * 
	 * @param viewpoint
	 * @param artist
	 * @return an ExpandedArtistView that includes albums made by the artist
	 * @throws NotFoundException
	 */
	public ExpandedArtistView expandedArtistSearch(Viewpoint viewpoint, String artist) throws NotFoundException;
	
	public List<PersonMusicView> getRelatedPeopleWithTracks(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<PersonMusicView> getRelatedPeopleWithAlbums(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<PersonMusicView> getRelatedPeopleWithArtists(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<AlbumView> getLatestAlbumViews(Viewpoint viewpoint, User user, int maxResults);
	
	public List<ArtistView> getLatestArtistViews(Viewpoint viewpoint, User user, int maxResults);
}
