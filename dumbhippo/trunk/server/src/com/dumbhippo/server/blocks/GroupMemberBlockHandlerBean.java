package com.dumbhippo.server.blocks;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class GroupMemberBlockHandlerBean extends AbstractBlockHandlerBean<GroupMemberBlockView> implements
		GroupMemberBlockHandler {

	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	private PersonViewer personViewer;
	
	public GroupMemberBlockHandlerBean() {
		super(GroupMemberBlockView.class);
	}

	@Override
	protected void populateBlockViewImpl(GroupMemberBlockView blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
		GroupView groupView;
		try {
			groupView = groupSystem.loadGroup(viewpoint, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new BlockNotVisibleException("Group for the block is not visible", e);
		}
		User user = identitySpider.lookupUser(block.getData2AsGuid());
		PersonView memberView = personViewer.getPersonView(viewpoint, user, PersonViewExtra.PRIMARY_RESOURCE);
		GroupMember member;
		try {
			member = groupSystem.getGroupMember(viewpoint, groupView.getGroup(), user);
		} catch (NotFoundException e) {
			// This could mean the group isn't visible normally, but since we already 
			// did loadGroup above, it should not. Instead, it probably means someone
			// was a follower and we stacked a block, then they removed themselves
			// so now they have no GroupMember.
			member = null;
		}
		blockView.setGroupView(groupView);
		blockView.setMemberView(memberView);
		blockView.setStatus(member != null ? member.getStatus() : MembershipStatus.NONMEMBER);
		if (member != null)
			blockView.setAdders(personViewer.viewUsers(viewpoint, member.getAdders()));
		blockView.setPopulated(true);
	}
}
