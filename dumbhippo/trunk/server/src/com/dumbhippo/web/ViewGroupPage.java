package com.dumbhippo.web;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonInfo;
import com.dumbhippo.server.PostInfo;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

public class ViewGroupPage {
	static private final Log logger = GlobalSetup.getLog(ViewGroupPage.class);	
	
	private Group viewedGroup;
	private String viewedGroupId;
	
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private GroupSystem groupSystem;
	
	public ViewGroupPage() throws NamingException {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
	}
	
	public List<PostInfo> getPostInfos() {
		assert viewedGroup != null;
		return postBoard.getGroupPostInfos(viewedGroup, signin.getUser(), 0);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public void setSignin(SigninBean signin) {
		this.signin = signin;
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
	
	public List<PersonInfo> getMembers() {
		return PersonInfo.sortedList(groupSystem.getMemberInfos(signin.getUser(), viewedGroupId));
	}
	
	public boolean getIsMember() {
		return groupSystem.isMember(viewedGroup, signin.getUser());
	}
}
