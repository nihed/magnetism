package com.dumbhippo.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.persistence.AmazonAlbumResult;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.YahooAlbumResult;
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
	
	public List<YahooAlbumResult> getYahooAlbumResultsSync(String artistId);
	
    public Future<List<YahooAlbumResult>> getYahooAlbumResultsAsync(String artistId);
		
	public TrackView getTrackView(Track track, long lastListen);
	
	public Future<TrackView> getTrackViewAsync(Track track, long lastListen);
	
	public Future<TrackView> getCurrentTrackViewAsync(Viewpoint viewpoint, User user) throws NotFoundException;

	public AlbumView getAlbumView(String album, String artist);
	
	public Future<AlbumView> getAlbumViewAsync(String album, String artist);
	
	public ArtistView getArtistView(Track track);
	
	/**
	 * Returns a view of an artist that includes artist's albums.
	 * 
	 * @param artist artist's name
	 * @param artistId if you don't know artistId, just pass in null for it
	 * @return view of an artist that includes artist's albums
	 */
	public ExpandedArtistView getExpandedArtistView(String artist, String artistId) throws NotFoundException;

	public Future<ArtistView> getArtistViewAsync(Track track);
}
