package com.dumbhippo.server.views;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;

public class MusicPersonBlockView extends BlockView {
	private PersonView userView;
	private TrackView track;
	
	public MusicPersonBlockView(Block block, UserBlockData ubd, PersonView userView, TrackView track) {
		super(block, ubd);
		this.userView = userView;
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
	
	public PersonView getUserView() {
		return userView;
	}
	
	public TrackView getTrackView() {
		return this.track;
	}

	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.appendEmptyNode("musicPerson",
				                "userId", userView.getIdentifyingGuid().toString());
	}

	public List<Object> getReferencedObjects() {
		return Collections.singletonList((Object)userView);
	}
}
