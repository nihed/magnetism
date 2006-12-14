package com.dumbhippo.server.blocks;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;

public class MusicChatBlockView extends AbstractPersonBlockView {
	public static final int RECENT_MESSAGE_COUNT = 3;
	
	private List<ChatMessageView> recentMessages;
	private TrackView trackView;
	
	public MusicChatBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public MusicChatBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	void populate(PersonView userView, TrackView trackView, List<ChatMessageView> recentMessages) {
		partiallyPopulate(userView);
		this.trackView = trackView;
		this.recentMessages = recentMessages;
		setPopulated(true);
	}
	
	public TrackView getTrackView() {
		return trackView; 
	}
	
	public List<ChatMessageView> getRecentMessages() {
		return recentMessages;
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement("musicChat",
				            "userId", getPersonSource().getUser().getId());
		trackView.writeToXmlBuilder(builder, "track");
		builder.openElement("recentMessages");
		for (ChatMessageView message : recentMessages) {
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
		return "Played";
	}

	public @Override String getSummaryLink() {
		TrackView tv = getPersonSource().getCurrentTrack();
		return tv.getArtistPageLink();
	}

	public @Override String getSummaryLinkText() {
		TrackView tv = getPersonSource().getCurrentTrack();
		return tv.getTruncatedName();		
	}
}
