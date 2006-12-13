package com.dumbhippo.web.pages;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.WebEJBUtil;

public abstract class AbstractGroupPage {

	protected static final Logger logger = GlobalSetup.getLogger(ViewGroupPage.class);
	
	@Signin
	protected SigninBean signin;
	protected IdentitySpider identitySpider;
	protected PersonViewer personViewer;
	protected GroupSystem groupSystem;
	protected InvitationSystem invitationSystem;

	private Group viewedGroup;
	private String viewedGroupId;
	private int invitations;
	private boolean fromInvite;
	private boolean justAdded;
	private Set<User> adders;
	private PersonView inviter;
	private GroupMember groupMember;

	protected AbstractGroupPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		personViewer = WebEJBUtil.defaultLookup(PersonViewer.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		invitations = -1;
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public Group getViewedGroup() {
		return viewedGroup;
	}

	public String getViewedGroupId() {
		return viewedGroupId;
	}

	public String getName() {
		return viewedGroup.getName();
	}

	public void setViewedGroupId(String groupId) {
		viewedGroupId = groupId;
				
		// FIXME: add getGroupMemberByGroupId (or replace getGroupMember), so that
		// we only do one lookup in the database. Careful: need to propagate
		// the handling of REMOVED members from lookupGroupById to getGroupMember
		
		if (groupId != null) {
			try {
				viewedGroup = groupSystem.lookupGroupById(signin.getViewpoint(), groupId);
			} catch (NotFoundException e) {
				viewedGroupId = null;
			}
		}
		
		if (viewedGroup != null && signin.isValid()) {
			UserViewpoint viewpoint = (UserViewpoint)signin.getViewpoint();
			
			try {
				groupMember = groupSystem.getGroupMember(signin.getViewpoint(), viewedGroup, viewpoint.getViewer());
			} catch (NotFoundException e) {
				groupMember = new GroupMember(viewedGroup, viewpoint.getViewer().getAccount(), MembershipStatus.NONMEMBER);
			}
			
			// If you view a group you were invited to, you get added; you can leave again and then 
			// you enter the REMOVED state where you can re-add yourself but don't get auto-added.
			if (groupMember.getStatus() == MembershipStatus.INVITED) {
				groupSystem.addMember(viewpoint.getViewer(), viewedGroup, viewpoint.getViewer());
				
				// reload the groupMember to have the new state
				try {
					groupMember = groupSystem.getGroupMember(signin.getViewpoint(), viewedGroup, viewpoint.getViewer());
				} catch (NotFoundException e) {
					groupMember = new GroupMember(viewedGroup, viewpoint.getViewer().getAccount(), MembershipStatus.NONMEMBER);
				}
	
				justAdded = true;
			}
	
			adders = groupMember.getAdders();
		}
	}

	public List<PersonView> getMembers(MembershipStatus status) {
		List<PersonView> result = PersonView.sortedList(groupSystem.getMembers(signin.getViewpoint(), viewedGroup, status, -1));
		if (result.isEmpty())
			return null;
		else
			return result;
	}

	public List<PersonView> getActiveMembers() {
		return getMembers(MembershipStatus.ACTIVE);
	}

	public List<PersonView> getInvitedMembers() {
		return getMembers(MembershipStatus.INVITED);
	}

	public GroupMember getGroupMember() {
		return groupMember;
	}

	public boolean isMember() {
		return getGroupMember().isParticipant();
	}

	public boolean getCanModify() {
		return getGroupMember().canModify();
	}

	public boolean getCanJoin() {
		return !isMember() && 
		       (viewedGroup.getAccess() == GroupAccess.PUBLIC ||
		        getGroupMember().getStatus() == MembershipStatus.REMOVED);
	}

	public boolean getCanLeave() {
		return isMember() || isInvitedNotAccepted();
	}

	public boolean getCanShare() {
		return isMember() && !isForum();
	}

	public boolean isForum() {
		return viewedGroup.getAccess() == GroupAccess.PUBLIC;
	}
	
	public boolean isPublic() {
		return viewedGroup.getAccess() == GroupAccess.PUBLIC_INVITE;
	}
	
	public boolean isPrivate() {
		return viewedGroup.getAccess() == GroupAccess.SECRET;		
	}

	public boolean isInvitedNotAccepted() {
		return getGroupMember().getStatus() == MembershipStatus.INVITED;
	}

	public boolean isJustAdded() {
		return justAdded;
	}

	public PersonView getInviter() {
		// TODO: display all the adders
		if (inviter == null && adders.iterator().hasNext()) {
			inviter = personViewer.getPersonView(signin.getViewpoint(), adders.iterator().next());	
		}
		
		return inviter;
	}

	public int getInvitations() {
		if (invitations < 0) {
			if (signin.isValid()) {
				UserViewpoint viewpoint = (UserViewpoint)signin.getViewpoint();
				invitations = invitationSystem.getInvitations(viewpoint.getViewer());
			} else {
				invitations = 0;
			}
		}
		return invitations;
	}

	public boolean isFromInvite() {
		return fromInvite;
	}

	public void setFromInvite(boolean fromInvite) {
		
		if (viewedGroup != null) {
			throw new RuntimeException("setViewedGroupId needs to be called after setFromInvite");
		}
		
		this.fromInvite = fromInvite;
	}

}
