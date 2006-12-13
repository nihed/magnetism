package com.dumbhippo.server.views;

public class ArtistView {

	private String name;
	
	ArtistView() {		
	}

	public ArtistView(String name) {
		this.name = name;
		
		if (name == null)
			name = "Unknown Artist";
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
