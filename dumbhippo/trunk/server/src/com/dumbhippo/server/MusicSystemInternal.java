package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ejb.Local;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.lucene.DocumentBuilder;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.ExternalAccountFeedListener;
import com.dumbhippo.server.views.AlbumView;
import com.dumbhippo.server.views.ArtistView;
import com.dumbhippo.server.views.ExpandedArtistView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.services.AmazonAlbumData;
import com.dumbhippo.services.YahooAlbumData;
import com.dumbhippo.services.YahooSongData;

/**
 * These are music system methods used within the ejb tier, but not useful
 * for the web tier. Mostly split out just to make MusicSystem easier to use,
 * so there isn't a bunch of cruft to look at.
 * 
 * @author hp
 *
 */
@BanFromWebTier
@Local
public interface MusicSystemInternal
	extends MusicSystem, ExternalAccountFeedListener {

	public Track getTrack(Map<String,String> properties);
	
	public void setCurrentTrack(User user, Track track);

	public void setCurrentTrack(User user, Map<String,String> properties);
	
	/**
	 * 
	 * Add this track as if it were one we have listened to, but don't set it as current.
	 * @param user who listened
	 * @param properties props of the track
	 */
	public void addHistoricalTrack(User user, Map<String,String> properties);
	
	/**
	 * Add this track as if it were one we have listened to, recording the date on which it was listened.
	 * 
	 * @param user who listened
	 * @param properties props of the track
	 * @param listenDate milliseconds since the epoch
	 */	
	public void addHistoricalTrack(User user, Map<String,String> properties, long listenDate);	
	
	/**
	 * Should be invoked once after setCurrentTrack or addHistoricalTrack are invoked.
	 * 
	 * @param userId id of user whose music was changed
	 */
	public void queueMusicChange(final Guid userId);	
		
	public int countTrackHistory(Viewpoint viewpoint, User user);
	
	public TrackView getTrackView(Track track, long lastListen);

	public Future<TrackView> getTrackViewAsync(long trackId, long lastListen);	
	
	public Future<TrackView> getTrackViewAsync(Track track, long lastListen);
	
	public Future<TrackView> getCurrentTrackViewAsync(Viewpoint viewpoint, User user) throws NotFoundException;
	
	public AlbumView getAlbumView(Future<YahooAlbumData> futureYahooAlbum, Future<AmazonAlbumData> futureAmazonAlbum);

	public AlbumView getAlbumView(Viewpoint viewpoint, YahooSongData yahooSong, YahooAlbumData yahooAlbum, Future<AmazonAlbumData> futureAmazonAlbum, Future<List<YahooSongData>> futureAlbumTracks);
	
	public Future<AlbumView> getAlbumViewAsync(Future<YahooAlbumData> futureYahooAlbum, Future<AmazonAlbumData> futureAmazonAlbum);
	
	public ArtistView getArtistView(Track track);
	
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
	 * Returns a view of an artist that includes artist's albums that fall into albumsByArtist pageable object criteria.
	 * 
	 * @param viewpoint
	 * @param artist artist's name
	 * @param albumsByArtist a Pageable object that contains information on what albums should be added to the artist view
	 * @return view of an artist that contains requested albums
	 * @throws NotFoundException
	 */
	public ExpandedArtistView getExpandedArtistView(Viewpoint viewpoint, String artist, Pageable<AlbumView> albumsByArtist) throws NotFoundException;
	
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
	
	public Future<ArtistView> getArtistViewAsync(Track track);
	
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
	 * Add all tracks in the database to the specified Lucene index. This is an internal implementation
	 * detail of TrackIndex.reindex().
	 * 
	 * @param writer a Lucene IndexWriter
	 * @param builder a DocumentBuilder to use to create Lucene Document objects from Track
	 * @throws IOException
	 */
	public void indexAllTracks(IndexWriter writer, DocumentBuilder<Track> builder) throws IOException;
}
