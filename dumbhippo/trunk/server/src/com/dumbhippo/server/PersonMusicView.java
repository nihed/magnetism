package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PersonMusicView {
	private PersonView person;
	private List<TrackView> tracks;
	private List<AlbumView> albums;
	
	public PersonMusicView() {
		this.tracks = new ArrayList<TrackView>();
		this.albums = new ArrayList<AlbumView>();
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
}
