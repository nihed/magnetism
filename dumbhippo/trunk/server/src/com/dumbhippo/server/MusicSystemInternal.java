package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ejb.Local;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.lucene.DocumentBuilder;

import com.dumbhippo.persistence.AmazonAlbumResult;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.YahooAlbumResult;
import com.dumbhippo.persistence.YahooArtistResult;
import com.dumbhippo.persistence.YahooSongDownloadResult;
import com.dumbhippo.persistence.YahooSongResult;

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
public interface MusicSystemInternal extends MusicSystem {

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
	
	public void hintNeedsRefresh(Track track);
	
	public AmazonAlbumResult getAmazonAlbumSync(final String album, final String artist); 
	
	public Future<AmazonAlbumResult> getAmazonAlbumAsync(String album, String artist);
	
	public List<YahooSongResult> getYahooSongResultsSync(Track track);
	
    public List<YahooSongResult> getYahooSongResultsSync(String albumId);
	
	public Future<List<YahooSongResult>> getYahooSongResultsAsync(Track track);
	
	public List<YahooSongDownloadResult> getYahooSongDownloadResultsSync(String songId);
	
	public Future<List<YahooSongDownloadResult>> getYahooSongDownloadResultsAsync(String songId);
	
	public List<YahooAlbumResult> getYahooAlbumResultsSync(YahooArtistResult artist, Pageable<AlbumView> albumsByArtist, YahooAlbumResult albumToExclude);
	
    public Future<List<YahooAlbumResult>> getYahooAlbumResultsAsync(YahooArtistResult artist, Pageable<AlbumView> albumsByArtist, YahooAlbumResult albumToExclude);
    
	public YahooAlbumResult getYahooAlbumSync(String albumId) throws NotFoundException;
	
	public Future<YahooAlbumResult> getYahooAlbumAsync(String albumId);
		
	public YahooArtistResult getYahooArtistResultSync(String artist, String artistId) throws NotFoundException;
	
	public TrackView getTrackView(Track track, long lastListen);
	
	public Future<TrackView> getTrackViewAsync(Track track, long lastListen);
	
	public Future<TrackView> getCurrentTrackViewAsync(Viewpoint viewpoint, User user) throws NotFoundException;
	
	public AlbumView getAlbumView(Future<YahooAlbumResult> futureYahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum);

	public AlbumView getAlbumView(YahooAlbumResult yahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum, Future<List<YahooSongResult>> futureAlbumTracks);
	
	public Future<AlbumView> getAlbumViewAsync(Future<YahooAlbumResult> futureYahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum);
	
	public ArtistView getArtistView(Track track);
	
	/**
	 * Returns a view of an artist that includes artist's albums that fall into albumsByArtist pageable object criteria.
	 * 
	 * @param artist artist's name
	 * @param artistId if you don't know artistId, just pass in null for it
	 * @param albumsByArtist a Pageable object that contains information on what albums should be added to the artist view
	 * @return view of an artist that contains requested albums
	 * @throws NotFoundException
	 */
	public ExpandedArtistView getExpandedArtistView(String artist, String artistId, Pageable<AlbumView> albumsByArtist) throws NotFoundException;
	
	/**
	 * Returns a view of an artist that includes artist's albums that fall into albumsByArtist pageable object criteria.
	 * Keeps the position of the requested album as a top album on the first page. 
	 *  
	 * @param album to display as part of this album's artist view
	 * @param albumsByArtist a Pageable object that contains information on what albums should be added to the artist view
	 * @return view of an artist that contains requested albums
	 * @throws NotFoundException
	 */
	public ExpandedArtistView getExpandedArtistView(YahooAlbumResult album, Pageable<AlbumView> albumsByArtist) throws NotFoundException;
	
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
