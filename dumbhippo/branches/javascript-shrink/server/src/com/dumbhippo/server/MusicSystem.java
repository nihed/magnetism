package com.dumbhippo.server;

import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.TrackFeedEntry;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.AlbumView;
import com.dumbhippo.server.views.ArtistView;
import com.dumbhippo.server.views.ExpandedArtistView;
import com.dumbhippo.server.views.PersonMusicView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Local
public interface MusicSystem {
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException;
	
	/**
	 * Retrieve the set of tracks that were played globally on the system most recently
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param pageable object providing range to retrieve and in which to store results
	 */
	public void pageLatestTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable);
	
	/**
	 * Retrieve the set of tracks that were played globally on the system most recently,
	 * return only tracks that are more filled in (i.e. have album art) if 
	 * filledTracksOnly is set to true.
	 * 
	 * @param viewpoint Viewpoint retrieving the information
	 * @param pageable object providing range to retrieve and in which to store results
	 * @param filledTracksOnly whether or not all results must be filled in according to
	 *                         certain requirements, currently the one requirement is that
	 *                         the result has album art
	 */
	public void pageLatestTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable, boolean filledTracksOnly);

	public List<TrackView> getPopularTrackViews(Viewpoint viewpoint, int maxResults);
	
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
	
	public void pageLatestTrackViews(Viewpoint viewpoint, Group group, Pageable<TrackView> pageable);
	
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
	
	/**
	 * Returns a track view for a matching track.
	 * 
	 * @param viewpoint
	 * @param artist
	 * @param album
	 * @param name
	 * @return a track view for a matching track
	 * @throws NotFoundException
	 */
	public TrackView songSearch(Viewpoint viewpoint, String artist, String album, String name) throws NotFoundException;
	
	/**
	 * Returns an ExpandedArtistView that includes albums made by the artist that fall into albumsByArtist 
	 * pageable object criteria. Sets the results field of albumsByArtist to contain the same albums
	 * as the returned artist view.
	 * 
	 * First, attempts to find a track with a matching artist among existing tracks. Queries an outside 
	 * web service if a track with a matching artist is not found locally. Throws a NotFoundException if 
	 * there is no matching artist.
	 * 
	 * @param viewpoint
	 * @param artist an artist name, must not be null
	 * @param albumsByArtist a Pageable object that contains information on what albums should be 
	 *        added to the artist view
	 * @return an ExpandedArtistView that contains requested albums
	 * @throws NotFoundException
	 */
	public ExpandedArtistView expandedArtistSearch(Viewpoint viewpoint, String artist, Pageable<AlbumView> albumsByArtist) throws NotFoundException;

	/**
	 * Returns an ExpandedArtistView that includes albums made by the artist that fall into albumsByArtist 
	 * pageable object criteria. Returns the album that matches specified artist, album and song as a top  
	 * album on the first page. Sets the results field of albumsByArtist to contain the same albums
	 * as the returned artist view.
	 * 
	 * Attempts to find a track that matches parameters among existing tracks. If a track is
	 * not found, attempts to find a matching CachedYahooSongData in the existing database cache. Currently,
	 * if neither succeeds, does not query an outside web service. Throws a NotFoundException if 
	 * there is no matching album/song stored locally.
	 * 
	 * @param viewpoint
	 * @param artist an artist name, must not be null
	 * @param album an album name
	 * @param name a song name
	 * @param albumsByArtist a Pageable object that contains information on what albums should be 
	 *        added to the artist view
	 * @return an ExpandedArtistView that contains requested albums
	 * @throws NotFoundException
	 */
	public ExpandedArtistView expandedArtistSearch(Viewpoint viewpoint, String artist, String album, String name, Pageable<AlbumView> pageable) throws NotFoundException;
	
	public List<PersonMusicView> getRelatedPeopleWithTracks(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<PersonMusicView> getRelatedPeopleWithAlbums(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<PersonMusicView> getRelatedPeopleWithArtists(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<AlbumView> getLatestAlbumViews(Viewpoint viewpoint, User user, int maxResults);
	
	public List<ArtistView> getLatestArtistViews(Viewpoint viewpoint, User user, int maxResults);
	
	/**
	 * Search the database of tracks using Lucene.
	 * 
	 * @param viewpoint the viewpoint being searched from
	 * @param queryString the search string to use, in Lucene syntax. The search
	 *   will be done across both the title and description fields
	 * @return a TrackSearchResult object representing the search; you should
	 *    check the getError() method of this object to determine if an error
	 *    occurred (such as an error parsing the query string) 
	 */
	public TrackSearchResult searchTracks(Viewpoint viewpoint, String queryString);
	
	/**
	 * Get a range of tracks from the result object returned from searchTracks(). 
	 * This is slightly more efficient than calling TrackSearchResult getTracks(),
	 * because we avoid some EJB overhead.
	 * 
	 * @param viewpoint the viewpoint for the returned TrackView objects; must be the same 
	 *        as the viewpoint passed in when calling searchTracks()
	 * @param
	 * @param start the index of the first track to retrieve (starting at zero)
	 * @param count the maximum number of items desired 
	 * @return a list of TrackView objects; may have less than count items when no more
	 *        are available. 
	 */
	public List<TrackView> getTrackSearchTracks(Viewpoint viewpoint, TrackSearchResult searchResult, int start, int count);
	
	public void addFeedTrack(User user, TrackFeedEntry entry, int entryPosition);
	
	public long getLatestPlayTime(Viewpoint viewpoint, User user);
}
