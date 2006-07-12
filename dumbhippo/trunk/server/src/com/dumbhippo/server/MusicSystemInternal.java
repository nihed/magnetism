package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ejb.Local;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.lucene.DocumentBuilder;

import com.dumbhippo.persistence.AccountFeed;
import com.dumbhippo.persistence.AmazonAlbumResult;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.TrackFeedEntry;
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
	
	public AmazonAlbumResult getAmazonAlbumSync(final String album, final String artist); 
	
	public Future<AmazonAlbumResult> getAmazonAlbumAsync(String album, String artist);
	
	public List<YahooSongResult> getYahooSongResultsSync(Track track);
	
    public List<YahooSongResult> getYahooSongResultsSync(String albumId);
	
	public Future<List<YahooSongResult>> getYahooSongResultsAsync(Track track);
	
	public List<YahooSongDownloadResult> getYahooSongDownloadResultsSync(String songId);
	
	public Future<List<YahooSongDownloadResult>> getYahooSongDownloadResultsAsync(String songId);
	
	public List<YahooAlbumResult> getYahooAlbumResultsSync(YahooArtistResult artist, Pageable<AlbumView> albumsByArtist, YahooAlbumResult albumToExclude);
	
    public Future<List<YahooAlbumResult>> getYahooAlbumResultsAsync(YahooArtistResult artist, Pageable<AlbumView> albumsByArtist, YahooAlbumResult albumToExclude);
    
	public YahooAlbumResult getYahooAlbumSync(YahooSongResult yahooSong) throws NotFoundException;
	
	public Future<YahooAlbumResult> getYahooAlbumAsync(YahooSongResult yahooSong);
		
	/*
	 * Returns a yahoo artist that matches an artist name and an artistId. Might return an artist with a different 
	 * name, if an artist with a different name matches a passed in artist id, or if multiple artist names match the
	 * same artist id, and we've been storing a different name mapped to the artistId that the passed in artist name
	 * also maps to. 
	 * 
	 * At least one parameter out of artist and artistId must not be null.
	 * 
	 * @param artist name of the artist
	 * @param artistId yahoo id for the artist
	 * @return YahooArtistResult that represents the artist
	 */
	public YahooArtistResult getYahooArtistResultSync(String artist, String artistId) throws NotFoundException;
	
	public TrackView getTrackView(Track track, long lastListen);
	
	public Future<TrackView> getTrackViewAsync(Track track, long lastListen);
	
	public Future<TrackView> getCurrentTrackViewAsync(Viewpoint viewpoint, User user) throws NotFoundException;
	
	public AlbumView getAlbumView(Future<YahooAlbumResult> futureYahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum);

	public AlbumView getAlbumView(Viewpoint viewpoint, YahooSongResult yahooSong, YahooAlbumResult yahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum, Future<List<YahooSongResult>> futureAlbumTracks);
	
	public Future<AlbumView> getAlbumViewAsync(Future<YahooAlbumResult> futureYahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum);
	
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
	public ExpandedArtistView getExpandedArtistView(Viewpoint viewpoint, YahooSongResult song, YahooAlbumResult album, Pageable<AlbumView> albumsByArtist) throws NotFoundException;
	
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
	
	public void addFeedTrack(AccountFeed feed, TrackFeedEntry entry, int entryPosition);
}
