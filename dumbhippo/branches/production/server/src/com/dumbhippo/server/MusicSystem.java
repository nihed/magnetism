package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ejb.Local;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.engine.DocumentBuilder;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.ExternalAccountFeedListener;
import com.dumbhippo.server.views.AlbumView;
import com.dumbhippo.server.views.ExpandedArtistView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.services.AmazonAlbumData;
import com.dumbhippo.services.YahooAlbumData;
import com.dumbhippo.services.YahooSongData;
import com.dumbhippo.tx.RetryException;

@Local
public interface MusicSystem extends ExternalAccountFeedListener {
	/**
	 * When we get a "native" music update (i.e. one from our XMPP) discard
	 * updates from other services if the listen time is within this timeout.
	 * 
	 * This should hopefully prevent duplicate updates.
	 */
	public static final long NATIVE_MUSIC_OVERRIDE_TIME_MS = 15 * 60 * 1000;	
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException;
	
	public boolean hasTrackHistory(Viewpoint viewpoint, User user);
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults);
	
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
	
	// somewhat oddly, returns 0 if none
	public long getLatestPlayTime(Viewpoint viewpoint, User user);
	
	public TrackHistory lookupTrackHistory(Guid trackHistoryId) throws NotFoundException;
	
	public Track getTrack(Map<String,String> properties) throws RetryException;

	public TrackView getTrackView(Track track);
	public TrackView getTrackView(TrackHistory trackHistory);
	
	public Future<TrackView> getTrackViewAsync(long trackId);	
	public Future<TrackView> getTrackViewAsync(TrackHistory trackHistory);

	/**
	 * Returns a view of an artist that includes artist's albums that fall into albumsByArtist pageable object criteria.
	 * Keeps the position of the requested album as a top album on the first page. 
	 *  
	 * @param viewpoint 
	 * @param song a song to expand when displaying the artist's view 
	 * @param album to display as the first album in the artist's view
	 * @param albumsByArtist a Pageable object that contains information on what albums should be added to the artist view
	 * @return view of an artist that contains requested albums
	 * @throws NotFoundException
	 */
	public ExpandedArtistView getExpandedArtistView(Viewpoint viewpoint, YahooSongData song, YahooAlbumData album, Pageable<AlbumView> albumsByArtist) throws NotFoundException;
	
	public AlbumView getAlbumView(Viewpoint viewpoint, YahooSongData yahooSong, YahooAlbumData yahooAlbum, Future<AmazonAlbumData> futureAmazonAlbum, Future<List<? extends YahooSongData>> futureAlbumTracks);

	/**
	 * Add the track given by a set of ids to the specified Lucene index. This is used internally
	 * when new tracks are created.
	 * 
	 * @param writer a Lucene IndexWriter
	 * @param builder a DocumentBuilder to use to create Lucene Document objects from Track
	 * @param ids a list of Post ids to index
	 * @throws IOException
	 */
	public void indexTracks(IndexWriter writer, DocumentBuilder<Track> builder, List<Object> ids) throws IOException;
	
	/**
	 * Get all track IDs. Used when reindexing. This is possible a bad idea, since
	 * we have half-a-million tracks right now.
	 *  
	 * @return all track IDs in the system
	 */
	public List<Long> getAllTrackIds();
	
	/**
	 * Add this track as if it were one we have listened to, recording the date on which it was listened.
	 * 
	 * @param user who listened
	 * @param properties props of the track
	 * @param listenDate milliseconds since the epoch
	 * @throws RetryException 
	 */	
	public void addHistoricalTrack(User user, Map<String,String> properties, long listenDate, boolean isNative) throws RetryException;
	
	/**
	 * 
	 * Add this track as if it were one we have listened to, but don't set it as current.
	 * @param user who listened
	 * @param properties props of the track
	 * @throws RetryException 
	 */
	public void addHistoricalTrack(User user, Map<String,String> properties, boolean isNative) throws RetryException;

	public void setCurrentTrack(User user, Track track, boolean isNative) throws RetryException;
	public void setCurrentTrack(User user, Map<String,String> properties, boolean isNative) throws RetryException;
}
