package com.dumbhippo.server.blocks;

import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class MusicChatBlockHandlerBean extends AbstractBlockHandlerBean<MusicChatBlockView> implements
		MusicChatBlockHandler {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(MusicChatBlockHandlerBean.class);
	
	@EJB
	MusicSystem musicSystem;
	
	public MusicChatBlockHandlerBean() {
		super(MusicChatBlockView.class);
	}

	public BlockKey getKey(TrackHistory trackHistory) {
		return new BlockKey(BlockType.MUSIC_CHAT, trackHistory.getUser().getGuid(), trackHistory.getGuid());
	}

	@Override
	protected void populateBlockViewImpl(MusicChatBlockView blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
		User user = getData1User(block);

		TrackHistory trackHistory;
		try {
			trackHistory = musicSystem.lookupTrackHistory(block.getData2AsGuid());
		} catch (NotFoundException e) {
			throw new BlockNotVisibleException(e.getMessage());
		}
		TrackView trackView = musicSystem.getTrackView(trackHistory);
		
		// no resource needed just to display user.getName()
		PersonView userView = personViewer.getPersonView(viewpoint, user);
		
		List<ChatMessageView> messageViews = chatSystem.viewMessages(chatSystem.getNewestMessages(block, MusicChatBlockView.RECENT_MESSAGE_COUNT), viewpoint);
		
		int messageCount;
		if (messageViews.size() < MusicChatBlockView.RECENT_MESSAGE_COUNT) // Optimize out a query
			messageCount = messageViews.size();
		else
			messageCount = chatSystem.getMessageCount(block);
		
		blockView.populate(userView, trackView, messageViews, messageCount);
	}
	
	public Set<User> getInterestedUsers(Block block) {
		return getUsersWhoCareAboutData1User(block);
	}

	public Set<Group> getInterestedGroups(Block block) {
		return getGroupsData1UserIsIn(block);
	}
}
