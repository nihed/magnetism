package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.List;

public class ExpandedArtistView extends ArtistView {

	private List<AlbumView> albums;
	
	ExpandedArtistView() {
		super();
	    albums = new ArrayList<AlbumView>();	
	}

	public ExpandedArtistView(String name) {
		super(name);
	    albums = new ArrayList<AlbumView>();
	}
	
	public List<AlbumView> getAlbums() {
		return albums;
	}
	
	public void addAlbum(AlbumView album) {
		// album.getArtist() should match this.getName()
		albums.add(album);
	}
}

