package com.dumbhippo.web;

import java.util.List;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

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
		return postBoard.getGroupPosts(signin.getViewpoint(), viewedGroup, 0);
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

	public void setViewedGroupId(String groupId) throws ParseException, GuidNotFoundException {
		viewedGroupId = groupId;
		
		if (groupId != null) {
			try {
				viewedGroup = identitySpider.lookupGuidString(Group.class, groupId);
			} catch (ParseException e) {
				viewedGroupId = null;
			} catch (GuidNotFoundException e) {
				viewedGroupId = null;
			}
		}
		
		if (viewedGroup != null) {
			groupMember = groupSystem.getGroupMember(signin.getViewpoint(), viewedGroup, signin.getUser());
			
			// Create a detached GroupMember to avoid null checks 
			if (groupMember == null)
				groupMember = new GroupMember(viewedGroup, signin.getUser(), MembershipStatus.NONMEMBER);

			// We don't want the whole page to go poof if you remove yourself, so you can 
			// still see the page if you were once a member, though you can't see all the
			// contents (unless you re-add yourself)
			if (viewedGroup.getAccess() == GroupAccess.SECRET &&
			    !(getGroupMember().getStatus().isMember() || 
			      getGroupMember().getStatus() == MembershipStatus.REMOVED)) {  
				viewedGroupId = null;
				viewedGroup = null;
				groupMember = null;
				
				return;
			}
			
			logger.debug("Status is " + groupMember.getStatus());
			if (groupMember.getStatus() == MembershipStatus.INVITED &&
			    groupMember.getAdder() != null) {
				inviter = identitySpider.getPersonView(signin.getViewpoint(), getGroupMember().getAdder());	
			}
		}
	}
	
	public List<PersonView> getMembers() {
		return PersonView.sortedList(groupSystem.getMembers(signin.getViewpoint(), viewedGroup));
	}
	
	public GroupMember getGroupMember() {
		return groupMember;
	}
	
	public boolean getIsMember() {
		return getGroupMember().getStatus().isMember();
	}
	
	public boolean getCanJoin() {
		return !getIsMember() && 
		       (viewedGroup.getAccess() == GroupAccess.PUBLIC ||
		        getGroupMember().getStatus() == MembershipStatus.REMOVED);
	}
	
	public PersonView getInviter() {
		return inviter;
	}
}
