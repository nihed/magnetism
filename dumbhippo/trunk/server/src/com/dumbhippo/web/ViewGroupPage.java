package com.dumbhippo.web;

import java.util.List;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

public class ViewGroupPage {
	static private final Log logger = GlobalSetup.getLog(ViewGroupPage.class);	
	
	private Group viewedGroup;
	private String viewedGroupId;

	@Signin
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private GroupSystem groupSystem;
	
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
	
	protected void setViewedGroup(Group group) {
		this.viewedGroup = group;
		this.viewedGroupId = group.getId();
		logger.debug("viewing Group: " + this.viewedGroupId);
	}
	
	public String getName() {
		return viewedGroup.getName();
	}

	public void setViewedGroupId(String groupId) throws ParseException, GuidNotFoundException {
		if (groupId == null) {
			logger.debug("no viewed group");
			return;
		} else {
			setViewedGroup(identitySpider.lookupGuidString(Group.class, groupId));
		}
	}
	
	public List<PersonView> getMembers() {
		return PersonView.sortedList(groupSystem.getMembers(signin.getViewpoint(), viewedGroup));
	}
	
	public boolean getIsMember() {
		if (signin.isValid())
			return groupSystem.isMember(signin.getViewpoint(), viewedGroup, signin.getUser());
		else
			return false;
	}
}
