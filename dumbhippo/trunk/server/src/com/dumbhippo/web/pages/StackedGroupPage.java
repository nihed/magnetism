package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.web.PagePositions;
import com.dumbhippo.web.PagePositionsBean;
import com.dumbhippo.web.WebEJBUtil;

public class StackedGroupPage extends AbstractSigninOptionalPage {
	protected static final Logger logger = GlobalSetup.getLogger(StackedGroupPage.class);
	
	static private final int INITIAL_BLOCKS_PER_PAGE = 5;
	static private final int SUBSEQUENT_BLOCKS_PER_PAGE = 20;
	
	private GroupSystem groupSystem;
	private Stacker stacker;
	
	@PagePositions
	PagePositionsBean pagePositions;
	
	private GroupView viewedGroup;
	private String viewedGroupId;
	private GroupMember groupMember;
	private Pageable<BlockView> pageableMugshot;
	private Pageable<BlockView> pageableStack;
		
	public StackedGroupPage() {		
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		stacker =  WebEJBUtil.defaultLookup(Stacker.class);
	}
	
	public GroupView getViewedGroup() {
		return viewedGroup;
	}

	public String getViewedGroupId() {
		return viewedGroupId;
	}

	public String getName() {
		return viewedGroup.getGroup().getName();
	}
	
	public String getNameAsHtml() {
		return XmlBuilder.escape(getName());
	}

	public void setViewedGroupId(String groupId) {
		viewedGroupId = groupId;
		
		Viewpoint viewpoint = getSignin().getViewpoint();
		Guid groupGuid;
		try {
			groupGuid = new Guid(viewedGroupId);
		} catch (ParseException e) {
			logger.debug("invalid group id");
			return;
		}
		
		try {
			viewedGroup = groupSystem.loadGroup(viewpoint, groupGuid);
		} catch (NotFoundException e) {
			logger.debug("invalid or inaccessible group id {}", groupId);
			return;
		}
		
		if (viewedGroup.getStatus() == MembershipStatus.INVITED || viewedGroup.getStatus() == MembershipStatus.INVITED_TO_FOLLOW ) {
			// Only UserViewpoints can have INVITED or INVITED_TO_FOLLOW membership status
			UserViewpoint userView = (UserViewpoint) viewpoint;
			groupSystem.acceptInvitation(userView, viewedGroup.getGroup());
			// Reload the view so we get the new status
			try {
				viewedGroup = groupSystem.loadGroup(viewpoint, groupGuid);
			} catch (NotFoundException e) {
				logger.debug("invalid or inaccessible group id {}", groupId);
				return;
			}
		}
		groupMember = viewedGroup.getGroupMember();
	}
	
	public GroupMember getGroupMember() {
		return groupMember;
	}

	public String getJoinAction() {
		if (!getSignin().isValid())
			return null;
			
		switch (getGroupMember().getStatus()) {
		case NONMEMBER:
		case INVITED_TO_FOLLOW:
			if (viewedGroup.getGroup().getAccess() == GroupAccess.PUBLIC)
				return "Join Group";
			else
				return "Follow Group";
		case INVITED:
		case REMOVED:
			return "Join Group";
		case ACTIVE:
		case FOLLOWER:
			return null;
		}
		return null;
	}
	
	public String getLeaveAction() {
		switch (getGroupMember().getStatus()) {
		case NONMEMBER:
		case REMOVED:			
			return null;
		case INVITED_TO_FOLLOW:
		case FOLLOWER:
			return "Stop Following";
		case INVITED:
		case ACTIVE:
			return "Leave Group";
		}
		return null;
	}
	
	public String getJoinTooltip() {
		if (!getSignin().isValid())
			return null;
			
		switch (getGroupMember().getStatus()) {
		case NONMEMBER:
		case INVITED_TO_FOLLOW:
			if (viewedGroup.getGroup().getAccess() == GroupAccess.PUBLIC)
				return "Become a group member";
			else
				return "Become a groupie! Get new stuff from this group.";
		case INVITED:
		case REMOVED:
			return "Become a group member";
		case FOLLOWER:
		case ACTIVE:
			return null;
		}
		return null;
	}	
	
	public String getLeaveTooltip() {
		switch (getGroupMember().getStatus()) {
		case NONMEMBER:
		case REMOVED:			
			return null;
		case INVITED_TO_FOLLOW:
		case FOLLOWER:
			return "Stop getting stuff from this group";
		case INVITED:
		case ACTIVE:
			return "I can't take it anymore! Let yourself out of this group.";
		}
		return null;
	}
	
	public boolean isMember() {
		return getGroupMember().isParticipant();
	}
	
	public boolean getCanAddMembers() {
		return (getGroupMember().canAddMembers() || isForum());
	}
	
	public boolean isFollower() {
		return getGroupMember().getStatus() == MembershipStatus.FOLLOWER;
	}
	
	public boolean getCanModify() {
		return getGroupMember().canModify();
	}

	public boolean getCanShare() {
		switch (getGroupMember().getStatus()) {
		case NONMEMBER:
		case INVITED_TO_FOLLOW:
		case INVITED:
		case REMOVED:
			return false;
		case ACTIVE:
		case FOLLOWER:
			return true;
		}
		return false;
	}

	public boolean isForum() {
		return viewedGroup.getGroup().getAccess() == GroupAccess.PUBLIC;
	}
	
	public boolean isPublic() {
		return viewedGroup.getGroup().getAccess() == GroupAccess.PUBLIC_INVITE;
	}
	
	public boolean isPrivate() {
		return viewedGroup.getGroup().getAccess() == GroupAccess.SECRET;		
	}

	public boolean isInvitedNotAccepted() {
		return getGroupMember().getStatus() == MembershipStatus.INVITED;
	}

	public Pageable<BlockView> getPageableMugshot() {
		if (pageableMugshot == null) {
		    pageableMugshot = pagePositions.createPageable("mugshot", INITIAL_BLOCKS_PER_PAGE); 
			pageableMugshot.setSubsequentPerPage(SUBSEQUENT_BLOCKS_PER_PAGE);
			pageableMugshot.setFlexibleResultCount(true);
			stacker.pageStack(getSignin().getViewpoint(), getViewedGroup().getGroup(), pageableMugshot, true);
		}

		return pageableMugshot;
	}

	public Pageable<BlockView> getPageableStack() {
		if (pageableStack == null) {
		    pageableStack = pagePositions.createPageable("stacker", INITIAL_BLOCKS_PER_PAGE); 
			pageableStack.setSubsequentPerPage(SUBSEQUENT_BLOCKS_PER_PAGE);
			pageableStack.setFlexibleResultCount(true);
			stacker.pageStack(getSignin().getViewpoint(), getViewedGroup().getGroup(), pageableStack, false);
		}

		return pageableStack;
	}
}
