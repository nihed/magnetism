package com.dumbhippo.server.blocks;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class GroupChatBlockHandlerBean extends AbstractBlockHandlerBean<GroupChatBlockView> implements
		GroupChatBlockHandler {

	@EJB
	private GroupSystem groupSystem;
	
	public GroupChatBlockHandlerBean() {
		super(GroupChatBlockView.class);
	}

	@Override
	protected void populateBlockViewImpl(GroupChatBlockView blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
		GroupView groupView;
		try {
			groupView = groupSystem.loadGroup(viewpoint, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new BlockNotVisibleException("Group for the block is not visible", e);
		}
		List<ChatMessageView> recentMessages = groupSystem.viewGroupMessages(
				groupSystem.getNewestGroupMessages(groupView.getGroup(), GroupChatBlockView.RECENT_MESSAGE_COUNT),
				viewpoint);
		blockView.setGroupView(groupView);
		blockView.setRecentMessages(recentMessages);
		blockView.setPopulated(true);
	}
}
