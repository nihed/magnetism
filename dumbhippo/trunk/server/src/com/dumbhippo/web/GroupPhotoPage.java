package com.dumbhippo.web;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

public class GroupPhotoPage {
	@SuppressWarnings("unused")
	static private final Log logger = GlobalSetup.getLog(ViewGroupPage.class);	
	
	private Group viewedGroup;
	private String viewedGroupId;

	@Signin
	private SigninBean signin;
	
	private GroupSystem groupSystem;	
	private GroupMember groupMember;
	
	public GroupPhotoPage() {		
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
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
		}
	}

	public GroupMember getGroupMember() {
		return groupMember;
	}
	
	public boolean getCanModify() {
		return getGroupMember().canModify();
	}
}
