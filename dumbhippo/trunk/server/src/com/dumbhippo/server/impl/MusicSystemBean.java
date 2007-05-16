package com.dumbhippo.server.impl;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.TrackSearchResult;
import com.dumbhippo.server.views.AlbumView;
import com.dumbhippo.server.views.ExpandedArtistView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;

/**
 * Originally there was one stateless bean implementing MusicSystem and MusicSystemInternal;
 * that seemed to not work, so I created this delegating bean.
 * 
 * @author Havoc Pennington
 *
 */
@Stateless
public class MusicSystemBean implements MusicSystem {

	@EJB
	private MusicSystemInternal internal;
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException {
		return internal.getCurrentTrackView(viewpoint, user);
	}

	public boolean hasTrackHistory(Viewpoint viewpoint, User user) {
		return internal.hasTrackHistory(viewpoint, user);
	}
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults) {
		return internal.getLatestTrackViews(viewpoint, user, maxResults);
	}

	public TrackView songSearch(Viewpoint viewpoint, String artist, String album, String name) throws NotFoundException {	
		return internal.songSearch(viewpoint, artist, album, name);
	}
	
	public ExpandedArtistView expandedArtistSearch(Viewpoint viewpoint, String artist, Pageable<AlbumView> albumsByArtist) throws NotFoundException {
		return internal.expandedArtistSearch(viewpoint, artist, albumsByArtist);
	}

	public ExpandedArtistView expandedArtistSearch(Viewpoint viewpoint, String artist, String album, String name, Pageable<AlbumView> albumsByArtist) throws NotFoundException {
		return internal.expandedArtistSearch(viewpoint, artist, album, name, albumsByArtist);
	}
	
	public TrackSearchResult searchTracks(Viewpoint viewpoint, String queryString) {
		return internal.searchTracks(viewpoint, queryString);
	}
	
	public List<TrackView> getTrackSearchTracks(Viewpoint viewpoint, TrackSearchResult searchResult, int start, int count) {
		return internal.getTrackSearchTracks(viewpoint, searchResult, start, count);
	}
	
	public long getLatestPlayTime(Viewpoint viewpoint, User user) {
		return internal.getLatestPlayTime(viewpoint, user);
	}

	public TrackHistory lookupTrackHistory(Guid trackHistoryId) throws NotFoundException {
		return internal.lookupTrackHistory(trackHistoryId);
	}
	
	public TrackView getTrackView(TrackHistory trackHistory) {
		return internal.getTrackView(trackHistory);
	}
	
	public List<Long> getAllTrackIds() {
		return internal.getAllTrackIds();
	}
}
