package com.dumbhippo.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.User;
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
public interface MusicSystemInternal {

	public Track getTrack(Map<String,String> properties);
	
	public void setCurrentTrack(User user, Track track);

	public void setCurrentTrack(User user, Map<String,String> properties);
	
	public void hintNeedsYahooResults(Track track);

	public List<YahooSongResult> getYahooSongResultsSync(Track track);
	
	public Future<List<YahooSongResult>> getYahooSongResultsAsync(Track track);
	
	public List<YahooSongDownloadResult> getYahooSongDownloadResultsSync(String songId);
	
	public Future<List<YahooSongDownloadResult>> getYahooSongDownloadResultsAsync(String songId);
	
	public TrackView getTrackView(Track track);
	
	public Future<TrackView> getTrackViewAsync(Track track);
	
	public Future<TrackView> getCurrentTrackViewAsync(Viewpoint viewpoint, User user) throws NotFoundException;

}
