package com.dumbhippo.server.views;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;

public class MusicPersonBlockView extends BlockView {
	
	private PersonView userView;
	
	public MusicPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, PersonView userView) {
		super(viewpoint, block, ubd);
		this.userView = userView;
		setPopulated(true);
	}

	public MusicPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
	}
	
	public PersonView getUserView() {
		return userView;
	}
	
	public void setUserView(PersonView userView) {
		this.userView = userView;
	}
	
	public List<TrackView> getTrackViews() {
		return userView.getTrackHistory(); 
	}

	@Override
	public PersonView getPersonSource() {
	    return userView;	
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.appendEmptyNode("musicPerson",
				                "userId", userView.getIdentifyingGuid().toString());
	}

	public List<Object> getReferencedObjects() {
		return Collections.singletonList((Object)userView);
	}
	
	@Override
	public String getIcon() {
		return "/images3/musicradar_icon.png";
	}
}
