package com.dumbhippo.server;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;

public class MusicPersonBlockView extends BlockView {
	private TrackView track;
	
	public MusicPersonBlockView(Block block, UserBlockData ubd, TrackView track) {
		super(block, ubd);
		this.track = track;
	}

	public String getWebTitleType() {
		return "Web Swarm";
	}
	
	public String getWebTitle() {
		return track.getName();
	}
	
	public String getIconName() {
		return "webswarm_icon.png";
	}
	
	public TrackView getTrackView() {
		return this.track;
	}
}
