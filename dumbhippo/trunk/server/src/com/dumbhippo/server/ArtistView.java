package com.dumbhippo.server;

import com.dumbhippo.persistence.Track;

public class ArtistView {

	private String name;
	
	ArtistView() {
		
	}
	
	public ArtistView(Track track) {
		this.name = track.getArtist();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
