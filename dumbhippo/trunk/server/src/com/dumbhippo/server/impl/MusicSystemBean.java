package com.dumbhippo.server.impl;

import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.TrackFeedEntry;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.TrackSearchResult;
import com.dumbhippo.server.views.AlbumView;
import com.dumbhippo.server.views.ArtistView;
import com.dumbhippo.server.views.ExpandedArtistView;
import com.dumbhippo.server.views.PersonMusicView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.UserViewpoint;
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

	public int countTrackHistory(Viewpoint viewpoint, User user) {
	    return internal.countTrackHistory(viewpoint, user);
	}
	
	public boolean hasTrackHistory(Viewpoint viewpoint, User user) {
		return internal.hasTrackHistory(viewpoint, user);
	}
	
	public void pageLatestTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable) {
		internal.pageLatestTrackViews(viewpoint, pageable);
	}
	
	public void pageLatestTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable, boolean filledTracksOnly) {
		internal.pageLatestTrackViews(viewpoint, pageable, filledTracksOnly);		
	}
	
	public List<TrackView> getPopularTrackViews(Viewpoint viewpoint, int maxResults) {
		return internal.getPopularTrackViews(viewpoint, maxResults);
	}
	
	public void pageLatestTrackViews(Viewpoint viewpoint, User user, Pageable<TrackView> pageable) {
		internal.pageLatestTrackViews(viewpoint, user, pageable);
	}
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults) {
		return internal.getLatestTrackViews(viewpoint, user, maxResults);
	}

	public void pageFrequentTrackViews(Viewpoint viewpoint, User user, Pageable<TrackView> pageable) {
		internal.pageFrequentTrackViews(viewpoint, user, pageable);
	}
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, User user, int maxResults) {
		return internal.getFrequentTrackViews(viewpoint, user, maxResults);
	}

	public void pageLatestTrackViews(Viewpoint viewpoint, Group group, Pageable<TrackView> pageable) {
		internal.pageLatestTrackViews(viewpoint, group, pageable);
	}
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, Group group, int maxResults) {
		return internal.getLatestTrackViews(viewpoint, group, maxResults);
	}

	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, Group group, int maxResults) {
		return internal.getFrequentTrackViews(viewpoint, group, maxResults);
	}
		
	public void pageFrequentTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable) {
		internal.pageFrequentTrackViews(viewpoint, pageable);
	}
	
	public void pageFrequentTrackViewsSince(Viewpoint viewpoint, Date since, Pageable<TrackView> pageable) {
		internal.pageFrequentTrackViewsSince(viewpoint, since, pageable);
	}

	public void pageOnePlayTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable) {
		internal.pageOnePlayTrackViews(viewpoint, pageable);
	}

	public void pageFriendsLatestTrackViews(UserViewpoint viewpoint, Pageable<TrackView> pageable) {
		internal.pageFriendsLatestTrackViews(viewpoint, pageable);
	}
	
	public List<PersonMusicView> getRelatedPeopleWithTracks(Viewpoint viewpoint, String artist, String album, String name) {
		return internal.getRelatedPeopleWithTracks(viewpoint, artist, album, name);
	}

	public List<PersonMusicView> getRelatedPeopleWithAlbums(Viewpoint viewpoint, String artist, String album, String name) {
		return internal.getRelatedPeopleWithAlbums(viewpoint, artist, album, name);
	}

	
	public List<AlbumView> getLatestAlbumViews(Viewpoint viewpoint, User user, int maxResults) {
		return internal.getLatestAlbumViews(viewpoint, user, maxResults);
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
	
	public List<PersonMusicView> getRelatedPeopleWithArtists(Viewpoint viewpoint, String artist, String album, String name) {
		return internal.getRelatedPeopleWithArtists(viewpoint, artist, album, name);
	}

	public List<ArtistView> getLatestArtistViews(Viewpoint viewpoint, User user, int maxResults) {
		return internal.getLatestArtistViews(viewpoint, user, maxResults);
	}
	
	public TrackSearchResult searchTracks(Viewpoint viewpoint, String queryString) {
		return internal.searchTracks(viewpoint, queryString);
	}
	
	public List<TrackView> getTrackSearchTracks(Viewpoint viewpoint, TrackSearchResult searchResult, int start, int count) {
		return internal.getTrackSearchTracks(viewpoint, searchResult, start, count);
	}
	
	public void addFeedTrack(User user, TrackFeedEntry entry, int entryPosition) {
		internal.addFeedTrack(user, entry, entryPosition);
	}
	
	public long getLatestPlayTime(Viewpoint viewpoint, User user) {
		return internal.getLatestPlayTime(viewpoint, user);
	}
}
