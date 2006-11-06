package com.dumbhippo.server.blocks;

import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class GroupChatBlockHandlerBean extends AbstractBlockHandlerBean<GroupChatBlockView> implements
		GroupChatBlockHandler {
	
	public GroupChatBlockHandlerBean() {
		super(GroupChatBlockView.class);
	}

	public BlockKey getKey(Group group) {
		return getKey(group.getGuid());
	}

	public BlockKey getKey(Guid groupId) {
		return new BlockKey(BlockType.GROUP_CHAT, groupId);
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
	
	public Set<User> getInterestedUsers(Block block) {
		return getUsersWhoCareAboutData1Group(block);
	}

	public Set<Group> getInterestedGroups(Block block) {
		return getData1GroupAsSet(block);
	}

	public void onGroupCreated(Group group) {
		Block block = stacker.createBlock(getKey(group));
		block.setPublicBlock(group.isPublic());
	}

	public void onGroupMessageCreated(GroupMessage message) {
		stacker.stack(getKey(message.getGroup()), message.getTimestamp().getTime(),
				message.getFromUser(), true);		
	}
}
