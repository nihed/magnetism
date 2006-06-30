package com.dumbhippo.server.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AlbumView;
import com.dumbhippo.server.ArtistView;
import com.dumbhippo.server.ExpandedArtistView;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonMusicView;
import com.dumbhippo.server.TrackSearchResult;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;

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
	
	public List<NowPlayingTheme> getExampleNowPlayingThemes(Viewpoint viewpoint, int maxResults) {
		return internal.getExampleNowPlayingThemes(viewpoint, maxResults);
	}
	
	public NowPlayingTheme createNewNowPlayingTheme(UserViewpoint viewpoint, NowPlayingTheme basedOn) {
		return internal.createNewNowPlayingTheme(viewpoint, basedOn);
	}
	
	public NowPlayingTheme getCurrentNowPlayingTheme(User user) throws NotFoundException {
		return internal.getCurrentNowPlayingTheme(user);
	}
	
	public void setCurrentNowPlayingTheme(UserViewpoint viewpoint, User user, NowPlayingTheme theme) {
		internal.setCurrentNowPlayingTheme(viewpoint, user, theme);
	}
	
	public NowPlayingTheme lookupNowPlayingTheme(String id) throws ParseException, NotFoundException {
		return internal.lookupNowPlayingTheme(id);
	}
	
	public NowPlayingTheme lookupNowPlayingTheme(Guid id) throws NotFoundException {
		return internal.lookupNowPlayingTheme(id);
	}
	
	public void setNowPlayingThemeImage(UserViewpoint viewpoint, String id, String type, String shaSum) throws NotFoundException, ParseException {
		internal.setNowPlayingThemeImage(viewpoint, id, type, shaSum);
	}
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException {
		return internal.getCurrentTrackView(viewpoint, user);
	}

	public void pageLatestTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable) {
		internal.pageLatestTrackViews(viewpoint, pageable);
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


	public NowPlayingTheme getCurrentTheme(Viewpoint viewpoint, User user) {
		return internal.getCurrentTheme(viewpoint, user);
	}


	public void getFriendsThemes(Viewpoint viewpoint, User user, Pageable<NowPlayingTheme> pageable) {
		internal.getFriendsThemes(viewpoint, user, pageable);
	}


	public void getMyThemes(Viewpoint viewpoint, User user, Pageable<NowPlayingTheme> pageable) {
		internal.getMyThemes(viewpoint, user, pageable);
	}


	public void getAllThemes(Viewpoint viewpoint, Pageable<NowPlayingTheme> pageable) {
		internal.getAllThemes(viewpoint, pageable);
	}
}
