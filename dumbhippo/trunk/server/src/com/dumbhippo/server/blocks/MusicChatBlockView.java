package com.dumbhippo.server.blocks;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;

public class MusicChatBlockView extends AbstractPersonBlockView implements MusicBlockView {
	
	static private final Logger logger = GlobalSetup.getLogger(MusicChatBlockView.class);
	
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
	public String getIcon() {
		return "/images3/musicradar_icon.png";
	}

	@Override
	public String getTypeTitle() {
		return "Music Radar";
	}

	// Since the stack reason for this type of block should alwayd be CHAT_MESSAGE, 
	// the following two methods should not end up being called, but rather the
	// corresponding methods in AbstractPersonBlockView should return the same values.
	public @Override String getBlockSummaryHeading() {
		logger.warn("in MusicChatBlockView::getBlockSummaryHeading() which is not expected, stack reason is {}", getStackReason());
		return "Chat about";
	}

	public @Override String getBlockSummaryLink() {
		logger.warn("in MusicChatBlockView::getBlockSummaryLink() which is not expected, stack reason is {}", getStackReason());
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
	
	@Override
	public String getChatKind() {
		return "music";
	}
}
