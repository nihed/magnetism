package com.dumbhippo.web;

import java.util.List;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

public class ViewGroupPage {
	@SuppressWarnings("unused")
	static private final Log logger = GlobalSetup.getLog(ViewGroupPage.class);	
	
	private Group viewedGroup;
	private String viewedGroupId;

	@Signin
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private GroupSystem groupSystem;
	private PersonView inviter;
	
	private GroupMember groupMember;
	
	public ViewGroupPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
	}
	
	public List<PostView> getPosts() {
		assert viewedGroup != null;
		return postBoard.getGroupPosts(signin.getViewpoint(), viewedGroup, 0, 10);
	}
	
	public SigninBean getSignin() {
		return signin;
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
			viewedGroup = groupSystem.lookupGroupById(signin.getViewpoint(), groupId);
			if (viewedGroup == null)
				viewedGroupId = null;
		}
		
		if (viewedGroup != null) {
			groupMember = groupSystem.getGroupMember(signin.getViewpoint(), viewedGroup, signin.getUser());
			
			// Create a detached GroupMember to avoid null checks 
			if (groupMember == null)
				groupMember = new GroupMember(viewedGroup, signin.getUser().getAccount(), MembershipStatus.NONMEMBER);

			if (groupMember.getStatus() == MembershipStatus.INVITED &&
			    groupMember.getAdder() != null) {
				inviter = identitySpider.getPersonView(signin.getViewpoint(), getGroupMember().getAdder());	
			}
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
	
	public boolean getIsMember() {
		return getGroupMember().isParticipant();
	}

	public boolean getCanModify() {
		return getGroupMember().canModify();
	}
	
	public boolean getCanJoin() {
		return !getIsMember() && 
		       (viewedGroup.getAccess() == GroupAccess.PUBLIC ||
		        getGroupMember().getStatus() == MembershipStatus.REMOVED);
	}
	
	public boolean getIsForum() {
		return viewedGroup.getAccess() == GroupAccess.PUBLIC;
	}
	
	public PersonView getInviter() {
		return inviter;
	}
}
