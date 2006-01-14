package com.dumbhippo.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.persistence.CurrentTrack;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.YahooSongDownloadResult;
import com.dumbhippo.persistence.YahooSongResult;

@Local
public interface MusicSystem {

	public Track getTrack(Map<String,String> properties);
	
	public void setCurrentTrack(User user, Track track);

	public void setCurrentTrack(User user, Map<String,String> properties);
	
	public CurrentTrack getCurrentTrack(Viewpoint viewpoint, User user) throws NotFoundException;
	
	public void hintNeedsYahooResults(Track track);

	public List<YahooSongResult> getYahooSongResultsSync(Track track);
	
	public Future<List<YahooSongResult>> getYahooSongResultsAsync(Track track);
	
	public List<YahooSongDownloadResult> getYahooSongDownloadResultsSync(String songId);
	
	public Future<List<YahooSongDownloadResult>> getYahooSongDownloadResultsAsync(String songId);
	
	public TrackView getTrackView(Track track);
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException;
	
	public Future<TrackView> getTrackViewAsync(Track track);
	
	public Future<TrackView> getCurrentTrackViewAsync(Viewpoint viewpoint, User user) throws NotFoundException;
}
