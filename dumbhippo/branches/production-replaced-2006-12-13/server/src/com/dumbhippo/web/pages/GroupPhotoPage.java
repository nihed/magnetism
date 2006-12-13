package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;
import com.dumbhippo.web.WebEJBUtil;

public class GroupPhotoPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ViewGroupPage.class);	
	
	private Group viewedGroup;
	private String viewedGroupId;

	@Signin
	private UserSigninBean signin;
	
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

	public void setViewedGroupId(String groupId) throws ParseException, NotFoundException {
		viewedGroupId = groupId;
		
		// FIXME: add getGroupMemberByGroupId (or replace getGroupMember), so that
		// we only do one lookup in the database. Careful: need to propagate
		// the handling of REMOVED members from lookupGroupById to getGroupMember
		
		if (groupId != null) {
			try {
			    viewedGroup = groupSystem.lookupGroupById(signin.getViewpoint(), groupId);
			} catch (NotFoundException e) {
		        viewedGroup = null;
			    viewedGroupId = null;
			}
		}
		
		if (viewedGroup != null) {
			try {
			    groupMember = groupSystem.getGroupMember(signin.getViewpoint(), viewedGroup, signin.getUser());
			} catch (NotFoundException e) {
			    // Create a detached GroupMember to avoid null checks 
				groupMember = new GroupMember(viewedGroup, signin.getUser().getAccount(), MembershipStatus.NONMEMBER);
			}
		}
	}

	public GroupMember getGroupMember() {
		return groupMember;
	}
	
	public boolean getCanModify() {
		return getGroupMember().canModify();
	}
}
