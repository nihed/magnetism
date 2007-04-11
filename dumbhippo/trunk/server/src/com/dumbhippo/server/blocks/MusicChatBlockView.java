package com.dumbhippo.server.blocks;

import java.util.ArrayList;
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

public class MusicChatBlockView extends AbstractPersonBlockView implements MusicBlockView {
	private TrackView trackView;
	
	public MusicChatBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public MusicChatBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	public void populate(PersonView userView, TrackView trackView, List<ChatMessageView> recentMessages, int messageCount) {
		partiallyPopulate(userView);
		this.trackView = trackView;
		setRecentMessages(recentMessages);
		setMessageCount(messageCount);
		setPopulated(true);
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement("musicChat",
				            "userId", getPersonSource().getUser().getId());
		trackView.writeToXmlBuilder(builder, "track");
		builder.openElement("recentMessages");
		for (ChatMessageView message : getRecentMessages()) {
			message.writeToXmlBuilder(builder);
		}
		builder.closeElement();
		builder.closeElement();
	}

	@Override
	public List<Object> getReferencedObjects() {
		List<Object> result = new ArrayList<Object>();
		result.add(getPersonSource());
		for (ChatMessageView message : getRecentMessages()) {
			result.add(message.getSenderView());
		}
		return result;
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
		return "Quip";
	}

	public @Override String getSummaryLink() {
		return getPersonSource().getHomeUrl();
	}

	public @Override String getSummaryLinkText() {
		return getTrack().getTruncatedName();		
	}

	//
	// MusicBlockView methods
	//
	public TrackView getTrack() {
		return trackView;
	}
	
	public List<TrackView> getOldTracks() {
		return Collections.emptyList();
	}

	public List<TrackView> getTrackViews() {
		return getPersonSource().getTrackHistory(); 
	}
	
	public boolean isQuip() {
		return true;
	}
	
	@Override
	public String getChatId() {
		return trackView.getPlayId();
	}
}
