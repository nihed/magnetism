package com.dumbhippo.server.blocks;

import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;

public class MusicPersonBlockView extends AbstractPersonBlockView {
	
	public MusicPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public MusicPersonBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
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
		builder.openElement("musicPerson",
				            "userId", getUserView().getUser().getId());
		builder.openElement("trackHistory");
		for (TrackView trackView : getUserView().getTrackHistory()) {
			trackView.writeToXmlBuilder(builder, "track");
		}
		builder.closeElement();
		builder.closeElement();
	}
	
	@Override
	public String getIcon() {
		return "/images3/musicradar_icon.png";
	}

	@Override
	public String getTypeTitle() {
		return "Music Radar";
	}

	public @Override String getSummaryHeading() {
		return "Played";
	}

	public @Override String getSummaryLink() {
		TrackView tv = getUserView().getCurrentTrack();
		return tv.getArtistPageLink();
	}

	public @Override String getSummaryLinkText() {
		TrackView tv = getUserView().getCurrentTrack();
		return tv.getTruncatedName();		
	}
}
