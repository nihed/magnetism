package com.dumbhippo.server.views;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.URLUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;

public class MusicPersonBlockView extends BlockView {
	private PersonView userView;
	
	public MusicPersonBlockView(Block block, UserBlockData ubd, PersonView userView) {
		super(block, ubd);
		this.userView = userView;
	}

	public String getWebTitleType() {
		return "Music Radar";
	}
	
	public String getWebTitle() {
		return getTrackView().getName();
	}
	
	@Override
	public String getWebTitleLink() {
		return URLUtils.buildUrl("/artist", "track", getTrackView().getName(),
								"album", getTrackView().getAlbum(),
								"artist", getTrackView().getArtist());
	}

	public String getIconName() {
		return "musicradar_icon.png";
	}
	
	public PersonView getUserView() {
		return userView;
	}
	
	public TrackView getTrackView() {
		return userView.getCurrentTrack();
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
