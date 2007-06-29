package com.dumbhippo.server.blocks;

import java.util.Set;

import javax.ejb.Stateless;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class GroupMemberBlockHandlerBean extends AbstractBlockHandlerBean<GroupMemberBlockView> implements
		GroupMemberBlockHandler {
	
	public GroupMemberBlockHandlerBean() {
		super(GroupMemberBlockView.class);
	}

	public BlockKey getKey(Group group, User user) {
		return getKey(group.getGuid(), user.getGuid());
	}
	
	public BlockKey getKey(Guid groupId, Guid userId) {		
		// Note that GroupMember objects can be deleted (and recreated).
		// They can also be associated with invited addresses and not accounts.
		// The identity of the Block is thus tied to the groupId,userId pair
		// rather than the GroupMember.

		return new BlockKey(BlockType.GROUP_MEMBER, groupId, userId);
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
		PersonView memberView = personViewer.getPersonView(viewpoint, user);
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
		
		MembershipStatus status = member != null ? member.getStatus() : MembershipStatus.NONMEMBER;

		// Determine if it's useful for the viewer to invite the person to the
		// group. This basically means that the user wants to be in the group
		// (status == FOLLOWER) and the user has the power to invite them in.
		// We also allow upgrading INVITED_TO_FOLLOW to INVITED, though 
		// I don't think that will come up in practice.
		boolean viewerCanInvite = false;
		if (status == MembershipStatus.FOLLOWER || status == MembershipStatus.INVITED_TO_FOLLOW) {
			if ((viewpoint instanceof UserViewpoint) && 
				groupSystem.canAddMembers(((UserViewpoint)viewpoint).getViewer(), groupView.getGroup()))
				viewerCanInvite = true;
		}

		blockView.populate(groupView, memberView, status,
				member != null ?
						personViewer.viewUsers(viewpoint, member.getAdders()) : null,
						viewerCanInvite);
	}
	
	public Set<User> getInterestedUsers(Block block) {
		Group group = em.find(Group.class, block.getData1AsGuid().toString());
		
		Set<User> recipients = groupSystem.getMembershipChangeRecipients(group);
		
		// We always want to notify users about changes to their own status,
		// even when they leave and are no longer a member
		User user = em.find(User.class, block.getData2AsGuid().toString());
		recipients.add(user);
		
		return recipients;
	}
	
	public Set<Group> getInterestedGroups(Block block) {
		return getData1GroupAsSet(block);
	}
	
	public void onGroupMemberCreated(GroupMember member, long when) {
		// Blocks only exist for group members which correspond to accounts in the
		// system. If the group member is (say) an email resource, and later joins
		// the system, when they join, we'll delete this GroupMember, add a new one 
		// for the Account and create a block for that GroupMember. 
		AccountClaim a = member.getMember().getAccountClaim();
		if (a != null) {
			// This is getOrCreate because a GroupMember can be deleted and then we'll 
			// get onGroupMemberCreated again later for the same group/person if they rejoin
			Block block = stacker.getOrCreateBlock(getKey(member.getGroup(), a.getOwner()));
			block.setPublicBlock(member.getGroup().isPublic());
			stacker.stack(block, when, a.getOwner(), true, StackReason.BLOCK_UPDATE);
		}		
	}

	public void onGroupMemberStatusChanged(GroupMember member, long when) {
		AccountClaim a = member.getMember().getAccountClaim();
		if (a == null)
			return; // ignore "resource" members
		
		switch (member.getStatus()) {
		case ACTIVE:
		case FOLLOWER:
		case REMOVED:
		case INVITED:
		case INVITED_TO_FOLLOW:			
			stacker.stack(getKey(member.getGroup(), a.getOwner()),
					when, a.getOwner(), true, StackReason.BLOCK_UPDATE);
			break;
		case NONMEMBER:
			// moves to these states don't create a new timestamp
			break;
			// don't add a default case, we want a warning if any are missing
		}
	}
}
