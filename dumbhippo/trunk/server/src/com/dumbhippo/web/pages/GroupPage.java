package com.dumbhippo.web.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupFeed;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.PagePositions;
import com.dumbhippo.web.PagePositionsBean;
import com.dumbhippo.web.WebEJBUtil;

public class GroupPage extends AbstractSigninOptionalPage {
	protected static final Logger logger = GlobalSetup.getLogger(GroupPage.class);
	
	// We override the default values for initial and subsequent results per page from Pageable
	// This number will apply to each section of members (active/followers/invited/invited followers)
	static private final int MEMBERS_PER_PAGE = 50;
	static private final int MAX_MEMBERS_SHOWN = 5;
	
	private PostingBoard postBoard;
	private MusicSystem musicSystem;
	private GroupSystem groupSystem;
	
	@PagePositions
	PagePositionsBean pagePositions;
	
	private GroupView viewedGroup;
	private String viewedGroupId;
	private boolean fromInvite;
	private GroupMember groupMember;
	private Pageable<TrackView> latestTracks;
	private ListBean<PersonView> activeMembers;
	private ListBean<PersonView> invitedMembers;
	private ListBean<PersonView> followers;
	private ListBean<PersonView> invitedFollowers;
	private Pageable<PersonView> pageableActiveMembers;
	private Pageable<PersonView> pageableInvitedMembers;
	private Pageable<PersonView> pageableFollowers;
	private Pageable<PersonView> pageableInvitedFollowers;
	private ListBean<GroupFeed> feeds;
	
	private Pageable<PostView> posts;
	private boolean allMembers;
	
	public GroupPage() {		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		
		allMembers = false;	
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
		
		// Once the user has accepted the terms of use, viewing a group should
		// implicitly accepts the invitation to the group
		if (getSignin().isActive() && 
			(viewedGroup.getStatus() == MembershipStatus.INVITED || viewedGroup.getStatus() == MembershipStatus.INVITED_TO_FOLLOW)) {
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
	
	public void setAllMembers(boolean allMembers) {
		this.allMembers = allMembers;
	}

	private List<PersonView> getMembers(MembershipStatus status) {
		int maxResults = allMembers ? -1 : MAX_MEMBERS_SHOWN;
		List<PersonView> result = PersonView.sortedList(groupSystem.getMembers(getSignin().getViewpoint(), viewedGroup.getGroup(), status, maxResults, PersonViewExtra.EXTERNAL_ACCOUNTS));
		return result;
	}
 
	private Pageable<PersonView> pageMembers(ListBean<PersonView> members, String pageableName) {
        Pageable<PersonView> pageableMembers = pagePositions.createPageable(pageableName);
		pageableMembers.setInitialPerPage(MEMBERS_PER_PAGE);
		pageableMembers.setSubsequentPerPage(MEMBERS_PER_PAGE);
		
		pageableMembers.generatePageResults(members.getList());
		
		return pageableMembers;
    }
	
	public ListBean<PersonView> getActiveMembers() {
		if (activeMembers == null)
			activeMembers = new ListBean<PersonView>(getMembers(MembershipStatus.ACTIVE));
		return activeMembers;
	}

	public Pageable<PersonView> getPageableActiveMembers() {
		if (pageableActiveMembers == null) {
            pageableActiveMembers = pageMembers(getActiveMembers(), "activeMembers");
		}
		return pageableActiveMembers;
	}
	
	public ListBean<PersonView> getInvitedMembers() {
		// FIXME the isMember() check is broken, it should be inside GroupSystem.getMembers()
		if (invitedMembers == null && isMember())
			invitedMembers = new ListBean<PersonView>(getMembers(MembershipStatus.INVITED));
		return invitedMembers;
	}

	public Pageable<PersonView> getPageableInvitedMembers() {
		if (pageableInvitedMembers == null) {
            pageableInvitedMembers = pageMembers(getInvitedMembers(), "invitedMembers");
		}
		return pageableInvitedMembers;
	}
	
	public ListBean<PersonView> getFollowers() {
		if (followers == null)
			followers = new ListBean<PersonView>(getMembers(MembershipStatus.FOLLOWER));
		return followers;
	}

	public Pageable<PersonView> getPageableFollowers() {
		if (pageableFollowers == null) {
            pageableFollowers = pageMembers(getFollowers(), "followers");
		}
		return pageableFollowers;
	}
	
	public ListBean<PersonView> getInvitedFollowers() {
		if (invitedFollowers == null)
			invitedFollowers = new ListBean<PersonView>(getMembers(MembershipStatus.INVITED_TO_FOLLOW));
		return invitedFollowers;
	}

	public Pageable<PersonView> getPageableInvitedFollowers() {
		if (pageableInvitedFollowers == null) {
            pageableInvitedFollowers = pageMembers(getInvitedFollowers(), "invitedFollowers");
		}
		return pageableInvitedFollowers;
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
	
	public String getShareSubject() {
		// sharing isn't actually enabled for all statuses, but no need to have that logic here,
		// just return a share subject for all cases
		if (getGroupMember().getStatus().ordinal() >= MembershipStatus.REMOVED.ordinal()) {
			return "Join the " + viewedGroup.getGroup().getName() + " group on Mugshot";
		} else {
			return "Follow the " + viewedGroup.getGroup().getName() + " group on Mugshot";
		}
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
	
	public boolean isFromInvite() {
		return fromInvite;
	}

	public void setFromInvite(boolean fromInvite) {
		
		if (viewedGroup != null) {
			throw new RuntimeException("setViewedGroupId needs to be called after setFromInvite");
		}
		
		this.fromInvite = fromInvite;
	}
	
	public ListBean<GroupFeed> getFeeds() {
		if (feeds == null) {
			List<GroupFeed> list = new ArrayList<GroupFeed>();
			for (GroupFeed feed : getViewedGroup().getGroup().getFeeds()) {
				if (!feed.isRemoved())
					list.add(feed);
			}
 
			Collections.sort(list, new Comparator<GroupFeed>() {

				public int compare(GroupFeed feed1, GroupFeed feed2) {
					return String.CASE_INSENSITIVE_ORDER.compare(feed1.getFeed().getSource().getUrl(),
							feed2.getFeed().getSource().getUrl());
				}
				
			});
			feeds = new ListBean<GroupFeed>(list);
		}
		return feeds;
	}
	
	public int getMaxMembersShown() {
		return MAX_MEMBERS_SHOWN;
	}
	
	// No longer used
	public Pageable<PostView> getPosts() {
		assert getViewedGroup() != null;
		
		if (posts == null) {
			posts = pagePositions.createPageable("groupPosts");
			postBoard.getGroupPosts(getSignin().getViewpoint(), getViewedGroup().getGroup(), posts);
		}
		return posts;
	}

    // No longer used
	public Pageable<TrackView> getLatestTracks() {
		if (latestTracks == null) {
			latestTracks = pagePositions.createPageable("latestTracks"); 
			musicSystem.pageLatestTrackViews(getSignin().getViewpoint(), getViewedGroup().getGroup(), latestTracks);
		}

		return latestTracks;
	}
}
