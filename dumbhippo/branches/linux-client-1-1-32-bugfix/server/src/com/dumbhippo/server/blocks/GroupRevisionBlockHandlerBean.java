package com.dumbhippo.server.blocks;

import java.util.Collections;
import java.util.Set;

import javax.ejb.Stateless;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupRevision;
import com.dumbhippo.persistence.Revision;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class GroupRevisionBlockHandlerBean
	extends AbstractBlockHandlerBean<GroupRevisionBlockView>
	implements GroupRevisionBlockHandler {

	protected GroupRevisionBlockHandlerBean() {
		super(GroupRevisionBlockView.class);
	}

	public BlockKey getKey(GroupRevision revision) {
		return new BlockKey(BlockType.GROUP_REVISION, null, null, revision.getId());
	}

	private GroupRevision getRevisionFromData3(Block block) {
		return (GroupRevision) em.find(Revision.class, block.getData3());
	}
	
	@Override
	protected void populateBlockViewImpl(GroupRevisionBlockView blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
		GroupRevision revision = getRevisionFromData3(block);
		
		GroupView groupView;
		try {
			groupView = groupSystem.loadGroup(viewpoint, revision.getTarget().getGuid());
		} catch (NotFoundException e) {
			throw new BlockNotVisibleException("Group for the block is not visible", e);
		}
		
		PersonView revisorView = personViewer.getPersonView(viewpoint, revision.getRevisor());

		blockView.populate(groupView, revisorView, revision);
	}

	public Set<User> getInterestedUsers(Block block) {
		GroupRevision revision = getRevisionFromData3(block);
		Set<User> groupMembers = groupSystem.getUserMembers(SystemViewpoint.getInstance(), revision.getTarget());
		return groupMembers;
	}

	public Set<Group> getInterestedGroups(Block block) {
		GroupRevision revision = getRevisionFromData3(block);
		return Collections.singleton(revision.getTarget());
	}

	public void onRevisionAdded(Revision revision) {
		if (!(revision instanceof GroupRevision))
			return;
		
		GroupRevision groupRevision = (GroupRevision) revision;
		
		Block block = stacker.createBlock(getKey(groupRevision));
		block.setPublicBlock(groupRevision.getTarget().isPublic());
		stacker.stack(block, groupRevision.getTimeAsLong(), groupRevision.getRevisor(), true, StackReason.NEW_BLOCK);
	}
}
