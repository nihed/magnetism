package com.dumbhippo.server.blocks;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;

public class MusicPersonBlockView extends AbstractPersonBlockView implements MusicBlockView {
	
	public MusicPersonBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public MusicPersonBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	public void populate(PersonView userView, List<ChatMessageView> recentMessages, int messageCount) {
		partiallyPopulate(userView);
		setRecentMessages(recentMessages);
		setMessageCount(messageCount);
		setPopulated(true);
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement("musicPerson",
				            "userId", getPersonSource().getUser().getId());
		builder.openElement("trackHistory");
		for (TrackView trackView : getPersonSource().getTrackHistory()) {
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
	public String getPrivacyTip() {
		return "Private: This music notification can only be seen by you.";
	}	
	
	@Override
	public String getTypeTitle() {
		return "Music Radar";
	}

	public @Override String getBlockSummaryHeading() {
		return "Played";
	}

	public @Override String getBlockSummaryLink() {
		TrackView tv = getPersonSource().getCurrentTrack();
		return tv.getArtistPageLink();
	}

	public @Override String getSummaryLinkText() {
		TrackView tv = getPersonSource().getCurrentTrack();
		return tv.getTruncatedName();		
	}

	//
	// MusicBlockView methods
	//
	public TrackView getTrack() {
		try {
			return getPersonSource().getTrackHistory().get(0);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public List<TrackView> getOldTracks() {
		List<TrackView> history = getPersonSource().getTrackHistory();
		if (history.size() < 2)
			return Collections.emptyList();
		else
			return history.subList(0, history.size() - 1);
	}

	public List<TrackView> getTrackViews() {
		return getPersonSource().getTrackHistory(); 
	}

	public boolean isQuip() {
		return false;
	}
	
	@Override
	public String getChatId() {
		try {
			return getPersonSource().getTrackHistory().get(0).getPlayId();
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	@Override
	public String getChatKind() {
		return "music";
	}
}
