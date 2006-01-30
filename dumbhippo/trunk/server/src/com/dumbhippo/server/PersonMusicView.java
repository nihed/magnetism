package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PersonMusicView {
	private PersonView person;
	private List<TrackView> tracks;
	
	PersonMusicView(PersonView person, List<TrackView> tracks) {
		this.person = person;
		this.tracks = new ArrayList<TrackView>(tracks);
	}
	
	public PersonView getPerson() {
		return person;
	}
	void setPerson(PersonView person) {
		this.person = person;
	}
	public List<TrackView> getTracks() {
		return Collections.unmodifiableList(tracks);
	}
	void setTracks(List<TrackView> tracks) {
		this.tracks = new ArrayList<TrackView>(tracks);
	}
	
	// needed since getTracks().size() can't be obtained easily in JSP expression language
	public int getTrackCount() {
		return this.tracks.size();
	}
}
