package com.dumbhippo.web;

import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.server.UserViewpoint;

public class GroupPage extends AbstractSigninOptionalPage {
	protected static final Logger logger = GlobalSetup.getLogger(GroupPage.class);
	
	static private final int MAX_POSTS_SHOWN = 10;
	static private final int MAX_MEMBERS_SHOWN = 5;
	
	private PostingBoard postBoard;
	private MusicSystem musicSystem;
	private Configuration configuration;
	private IdentitySpider identitySpider;
	private GroupSystem groupSystem;
	
	@PagePositions
	PagePositionsBean pagePositions;
	
	private GroupView viewedGroup;
	private String viewedGroupId;
	private boolean fromInvite;
	private boolean justAdded;
	private User adder;
	private PersonView inviter;
	private GroupMember groupMember;
	private ListBean<TrackView> latestTracks;
	private ListBean<PersonView> activeMembers;
	private ListBean<PersonView> invitedMembers;

	private Pageable<PostView> posts;
	private boolean allMembers;
	
	public GroupPage() {		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
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
				
		// FIXME: add getGroupMemberByGroupId (or replace getGroupMember), so that
		// we only do one lookup in the database. Careful: need to propagate
		// the handling of REMOVED members from lookupGroupById to getGroupMember
		
		Group group = null;
		if (groupId != null) {
			try {
				group = groupSystem.lookupGroupById(getSignin().getViewpoint(), groupId);
			} catch (NotFoundException e) {
				viewedGroupId = null;
			}
		}
		
		if (group != null) {
			if (getSignin().isValid()) {
				UserViewpoint viewpoint = (UserViewpoint)getSignin().getViewpoint();
						
				try {
					groupMember = groupSystem.getGroupMember(getSignin().getViewpoint(), group, viewpoint.getViewer());
				} catch (NotFoundException e) {
					groupMember = new GroupMember(group, viewpoint.getViewer().getAccount(), MembershipStatus.NONMEMBER);
				}
				
				viewedGroup = new GroupView(group, groupMember, null);			
				
				// If you view a group you were invited to, you get added; you can leave again and then 
				// you enter the REMOVED state where you can re-add yourself but don't get auto-added.
				if (groupMember.getStatus() == MembershipStatus.INVITED) {
					groupSystem.addMember(viewpoint.getViewer(), group, viewpoint.getViewer());
					
					// reload the groupMember to have the new state
					try {
						groupMember = groupSystem.getGroupMember(getSignin().getViewpoint(), group, viewpoint.getViewer());
					} catch (NotFoundException e) {
						groupMember = new GroupMember(group, viewpoint.getViewer().getAccount(), MembershipStatus.NONMEMBER);
					}
		
					justAdded = true;
				}
		
				adder = groupMember.getAdder();
			} else {
				groupMember = new GroupMember(group, null, MembershipStatus.NONMEMBER);
				viewedGroup = new GroupView(group, groupMember, null);
			}
		}
	}
	
	public void setAllMembers(boolean allMembers) {
		this.allMembers = allMembers;
	}

	private List<PersonView> getMembers(MembershipStatus status) {
		int maxResults = allMembers ? -1 : MAX_MEMBERS_SHOWN;
		List<PersonView> result = PersonView.sortedList(groupSystem.getMembers(getSignin().getViewpoint(), viewedGroup.getGroup(), status, maxResults));
		return result;
	}
 
	public ListBean<PersonView> getActiveMembers() {
		if (activeMembers == null)
			activeMembers = new ListBean<PersonView>(getMembers(MembershipStatus.ACTIVE));
		return activeMembers;
	}

	public ListBean<PersonView> getInvitedMembers() {
		if (invitedMembers == null)
			invitedMembers = new ListBean<PersonView>(getMembers(MembershipStatus.INVITED));
		return invitedMembers;
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
		       (viewedGroup.getGroup().getAccess() == GroupAccess.PUBLIC ||
		        getGroupMember().getStatus() == MembershipStatus.REMOVED);
	}

	public boolean getCanLeave() {
		return isMember() || isInvitedNotAccepted();
	}

	public boolean getCanShare() {
		return isMember() && !isForum();
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

	public boolean isJustAdded() {
		return justAdded;
	}

	public PersonView getInviter() {
		if (inviter == null && adder != null) {
			inviter = identitySpider.getPersonView(getSignin().getViewpoint(), adder);	
		}
		
		return inviter;
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
	
	public Pageable<PostView> getPosts() {
		assert getViewedGroup() != null;
		
		if (posts == null) {
			posts = pagePositions.createPageable("groupPosts");
			postBoard.getGroupPosts(getSignin().getViewpoint(), getViewedGroup().getGroup(), posts);
		}
		return posts;
	}

	public ListBean<TrackView> getLatestTracks() {
		if (latestTracks == null) {
			List<TrackView> tracks = musicSystem.getLatestTrackViews(getSignin().getViewpoint(), getViewedGroup().getGroup(), 3);
			latestTracks = new ListBean<TrackView>(tracks);
		}

		return latestTracks;
	}	
	
	
	public int getMaxPostsShown() {
		return MAX_POSTS_SHOWN;
	}
	
	public int getMaxMembersShown() {
		return MAX_MEMBERS_SHOWN;
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getProperty(HippoProperty.DOWNLOADURL_WINDOWS);
	}
}
