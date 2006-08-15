package com.dumbhippo.xmppcom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmppEventPrimingTracks extends XmppEvent {
	private static final long serialVersionUID = 1L;
	
	private List<Map<String,String>> tracks;
	private String jabberId;
	
	public XmppEventPrimingTracks(String jabberId) {
		this.jabberId = jabberId;
		tracks = new ArrayList<Map<String,String>>();
	}
	
	public String getJabberId() {
		return jabberId;
	}
	
	/**
	 * These are returned in order from most to least recent/frequent
	 * @return a list of tracks that represent the user's tastes
	 */
	public List<Map<String,String>> getTracks() {
		return tracks;
	}
	
	public void addTrack(Map<String,String> properties) {
		tracks.add(new HashMap<String,String>(properties));
	}
}
