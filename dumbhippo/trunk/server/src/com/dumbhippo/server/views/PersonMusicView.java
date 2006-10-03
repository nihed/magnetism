package com.dumbhippo.server.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PersonMusicView {
	private PersonView person;
	private List<TrackView> tracks;
	private List<AlbumView> albums;
	private List<ArtistView> artists;

	
	public PersonMusicView() {
		this.tracks = new ArrayList<TrackView>();
		this.albums = new ArrayList<AlbumView>();
		this.artists = new ArrayList<ArtistView>();
	}
	
	public PersonMusicView(PersonView person) {
		this();
		this.person = person;
	}

	// person is null if this is an "anonymous" PersonMusicView
	public PersonView getPerson() {
		return person;
	}
	public void setPerson(PersonView person) {
		this.person = person;
	}
	public List<TrackView> getTracks() {
		return Collections.unmodifiableList(tracks);
	}
	public void setTracks(List<TrackView> tracks) {
		this.tracks.clear();
		this.tracks.addAll(tracks);
	}
	public void addTrack(TrackView track) {
		this.tracks.add(track);
	}
	
	// needed since getTracks().size() can't be obtained easily in JSP expression language
	public int getTrackCount() {
		return this.tracks.size();
	}
	
	public List<AlbumView> getAlbums() {
		return Collections.unmodifiableList(albums);
	}
	public void setAlbums(List<AlbumView> albums) {
		this.albums.clear();
		this.albums.addAll(albums);
	}
	public void addAlbum(AlbumView album) {
		this.albums.add(album);
	}
	
	// needed since getAlbums().size() can't be obtained easily in JSP expression language
	public int getAlbumCount() {
		return this.albums.size();
	}

	public List<ArtistView> getArtists() {
		return Collections.unmodifiableList(artists);
	}
	public void setArtists(List<ArtistView> artists) {
		this.artists.clear();
		this.artists.addAll(artists);
	}
	public void addArtist(ArtistView artist) {
		this.artists.add(artist);
	}
	
	// needed since getArtists().size() can't be obtained easily in JSP expression language
	public int getArtistCount() {
		return this.artists.size();
	}
}
