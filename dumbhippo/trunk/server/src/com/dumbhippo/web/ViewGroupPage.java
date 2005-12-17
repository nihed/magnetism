package com.dumbhippo.web;

import java.util.List;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

public class ViewGroupPage {
	@SuppressWarnings("unused")
	static private final Log logger = GlobalSetup.getLog(ViewGroupPage.class);	
	
	static private final int MAX_POSTS_SHOWN = 10;
	
	private Group viewedGroup;
	private String viewedGroupId;
	
	@Signin
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private GroupSystem groupSystem;
	private InvitationSystem invitationSystem;
	private Configuration configuration;
	
	private int invitations;
	// fromInvite is whether we got here after just joining DumbHippo
	private boolean fromInvite;
	// the inviter here though is to the group, not necessarily to the site
	private User adder;
	private PersonView inviter;
	private GroupMember groupMember;
	
	public ViewGroupPage() {
		logger.debug("Constructing ViewGroupPage");
		
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
		invitations = -1;
	}
	
	public List<PostView> getPosts() {
		assert viewedGroup != null;
		
		// we ask for 1 extra post to see if we need a "more posts" link
		return postBoard.getGroupPosts(signin.getViewpoint(), viewedGroup, 0, MAX_POSTS_SHOWN + 1);
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
		
		if (viewedGroup != null) {
			// kind of a hack, but when people just accepted
			// the invitation we want to put them right in the group.
			if (fromInvite) {
				groupSystem.addMember(signin.getUser(), viewedGroup, signin.getUser());
			}
			
			try {
				groupMember = groupSystem.getGroupMember(signin.getViewpoint(), viewedGroup, signin.getUser());
			} catch (NotFoundException e) {
				groupMember = new GroupMember(viewedGroup, signin.getUser().getAccount(), MembershipStatus.NONMEMBER);
			}

			adder = groupMember.getAdder();
		}
	}
	
	public List<PersonView> getMembers(MembershipStatus status) {
		List<PersonView> result = PersonView.sortedList(groupSystem.getMembers(signin.getViewpoint(), viewedGroup, status));
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

	public boolean isInvitedNotAccepted() {
		return getGroupMember().getStatus() == MembershipStatus.INVITED;
	}
	
	public PersonView getInviter() {
		if (inviter == null && adder != null) {
			inviter = identitySpider.getPersonView(signin.getViewpoint(), adder);	
		}
		
		return inviter;
	}
	
	public int getMaxPostsShown() {
		return MAX_POSTS_SHOWN;
	}
	
	public int getInvitations() {
		if (invitations < 0) {
			invitations = invitationSystem.getInvitations(signin.getUser()); 
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
	
	public String getDownloadUrlWindows() {
		return configuration.getProperty(HippoProperty.DOWNLOADURL_WINDOWS);
	}
}
