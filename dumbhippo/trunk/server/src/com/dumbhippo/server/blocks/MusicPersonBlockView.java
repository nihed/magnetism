package com.dumbhippo.server.blocks;

import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;

public class MusicPersonBlockView extends AbstractPersonBlockView {
	
	public MusicPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, PersonView userView) {
		super(viewpoint, block, ubd, userView);
		// we have nothing to populate except the userView, so go ahead and mark populated
		setPopulated(true);
	}

	public MusicPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
	}
	
	void populate(PersonView userView) {
		partiallyPopulate(userView);
		setPopulated(true);
	}
	
	public List<TrackView> getTrackViews() {
		return getUserView().getTrackHistory(); 
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.appendEmptyNode("musicPerson",
				                "userId", getUserView().getIdentifyingGuid().toString());
	}
	
	@Override
	public String getIcon() {
		return "/images3/musicradar_icon.png";
	}
}
