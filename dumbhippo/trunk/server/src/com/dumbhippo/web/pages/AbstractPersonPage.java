package com.dumbhippo.web.pages;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.InvitationView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.PagePositions;
import com.dumbhippo.web.PagePositionsBean;
import com.dumbhippo.web.WebEJBUtil;

public abstract class AbstractPersonPage extends AbstractSigninOptionalPage {
	static private final Logger logger = GlobalSetup.getLogger(AbstractPersonPage.class);	
	
	// We override the default values for initial and subsequent results per page from Pageable
	static private final int FRIENDS_PER_PAGE = 20;
	static private final int GROUPS_PER_PAGE = 20;
	
	@PagePositions
	PagePositionsBean pagePositions;
	
	private User viewedUser;
	private String viewedUserId;
	private boolean disabled;
	private boolean needExternalAccounts;
	
	private GroupSystem groupSystem;
	private MusicSystem musicSystem;
	private PersonView viewedPerson;
	private AccountSystem accountSystem; 	
	
	private ListBean<GroupView> groups;
	private Pageable<GroupView> pageablePublicGroups;
	private ListBean<GroupView> followedGroups;
	private ListBean<GroupView> invitedGroups;
	private ListBean<GroupView> invitedToFollowGroups;
	private ListBean<GroupView> combinedGroups;
	
	// information about existing outstanding invitations
	private ListBean<InvitationView> outstandingInvitations;
	
	private boolean lookedUpCurrentTrack;
	private TrackView currentTrack;
	
	protected ListBean<PersonView> contacts;
	private Pageable<PersonView> pageableContacts; 
	protected int totalContacts;

	protected ListBean<PersonView> followers;
	private Pageable<PersonView> pageableFollowers;
	
	protected AbstractPersonPage() {	
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
		accountSystem = WebEJBUtil.defaultLookup(AccountSystem.class);
		lookedUpCurrentTrack = false;
	}
	
	protected IdentitySpider getIdentitySpider() { 
		return identitySpider;
	}
	
	protected AccountSystem getAccountSystem() {
		return accountSystem;
	}
	
	protected GroupSystem getGroupSystem() {
		return groupSystem;
	}
	
	protected MusicSystem getMusicSystem() {
		return musicSystem;
	}
 	
	public String getViewedUserId() {
		return viewedUserId;
	}
	
	public User getViewedUser() {
		return viewedUser;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	
	protected void setViewedUser(User user) {
		this.viewedUser = user;
		this.viewedUserId = user.getId();
		
		if (identitySpider.getAccountDisabled(user)) {
				this.disabled = true;
		}
		
		logger.debug("viewing person: {} disabled = {}", this.viewedUser, disabled);
	}
	
	public String getName() {
		return getViewedUser().getNickname();
	}

	public void setViewedUserId(String userId) {
		if (userId == null) {
			logger.debug("no viewed person");
			return;
		} else {
			try {
				setViewedUser(identitySpider.lookupGuidString(User.class, userId));
			} catch (ParseException e) {
				logger.debug("bad userId as person parameter {}", userId);
			} catch (NotFoundException e) {
				logger.debug("bad userId as person parameter {}", userId);
			}
		}
	}

	public PersonView getViewedPerson() {
		if (viewedPerson == null) {
			if (getNeedExternalAccounts())
				viewedPerson = personViewer.getPersonView(getSignin().getViewpoint(), getViewedUser(), PersonViewExtra.ALL_RESOURCES,
						PersonViewExtra.EXTERNAL_ACCOUNTS);
			else
				viewedPerson = personViewer.getPersonView(getSignin().getViewpoint(), getViewedUser(), PersonViewExtra.ALL_RESOURCES);
		}
		
		return viewedPerson;
	}
	
	public boolean isContact() {
		// this determines if the viewed user is a contact of the signed in user, it is used to decide 
		// whether to offer a link to add or to remove viewed user as a contact of the signed in user
		if (getSignin().isValid())
			return identitySpider.isContact(getSignin().getViewpoint(), getUserSignin().getUser(), getViewedUser());
		else
			return false;
	}
	
	public boolean isSelf() {
		if (getSignin().isValid() && getViewedUser() != null) {
			return getUserSignin().getUser().equals(getViewedUser());
		} else {
			return false;
		}
	}
	
	public boolean isValid() {
		return getViewedUser() != null;
	}
	
	// We don't show group's you haven't accepted the invitation for on your public page
	public ListBean<GroupView> getGroups() {
		if (groups == null) {
			groups = new ListBean<GroupView>(GroupView.sortedList(groupSystem.findGroups(getSignin().getViewpoint(), getViewedUser(), MembershipStatus.ACTIVE)));
		}
		return groups;
	}                                       
	
	public Pageable<GroupView> getPageablePublicGroups() {
        if (pageablePublicGroups == null) {			
			pageablePublicGroups = pagePositions.createPageable("publicGroups"); 				
			pageablePublicGroups.setInitialPerPage(GROUPS_PER_PAGE);
			pageablePublicGroups.setSubsequentPerPage(GROUPS_PER_PAGE);
			
			groupSystem.pagePublicGroups(pageablePublicGroups);
		}
		
		return pageablePublicGroups;
	}
	
	public ListBean<GroupView> getInvitedGroups() {
		// Only the user can see their own invited groups
		// FIXME this is broken, the access control rules need to be inside findGroups() ... but they are 
		// somewhat complex since people who are already in the group can see invited members, something
		// we aren't handling here
		if (!isSelf())
			return null;
		if (invitedGroups == null) {
			invitedGroups = new ListBean<GroupView>(GroupView.sortedList(groupSystem.findGroups(getSignin().getViewpoint(), getViewedUser(), MembershipStatus.INVITED)));
		}
		return invitedGroups;
	}
	
	public ListBean<GroupView> getFollowedGroups() {
		if (followedGroups == null) {
			followedGroups = new ListBean<GroupView>(GroupView.sortedList(groupSystem.findGroups(getSignin().getViewpoint(), getViewedUser(), MembershipStatus.FOLLOWER)));
		}
		return followedGroups;
	}

	public ListBean<GroupView> getInvitedToFollowGroups() {
		// Only the user can see their own invited groups
		// FIXME this is broken, the access control rules need to be inside findGroups() ... but they are 
		// somewhat complex since people who are already in the group can see invited members, something
		// we aren't handling here
		if (!isSelf())
			return null;		
		if (invitedToFollowGroups == null) {
			invitedToFollowGroups = new ListBean<GroupView>(GroupView.sortedList(groupSystem.findGroups(getSignin().getViewpoint(), getViewedUser(), MembershipStatus.INVITED_TO_FOLLOW)));
		}
		return invitedToFollowGroups;
	}	

	/**
	 * Combined list of groups the person is in and groups the person follows; 
	 * used for example in the Groups sideBox
	 * 
	 * Note: All member groups will appear in sorted order before all followed
	 * groups.
	 */
	public ListBean<GroupView> getCombinedGroups() {
		if (combinedGroups == null) {
			List<GroupView> groupsList = getGroups().getList();
			List<GroupView> followedGroupsList = getFollowedGroups().getList();
			List<GroupView> combinedGroupsList = new ArrayList<GroupView>(groupsList);
			combinedGroupsList.addAll(followedGroupsList);
			combinedGroups = new ListBean<GroupView>(combinedGroupsList);
		}
		return combinedGroups;
	}   
	
	public boolean isNewGroupInvites() {
		if (!isSelf())
			return false;
		Account acct = getViewedUser().getAccount();
		Date groupInvitationReceived = acct.getGroupInvitationReceived();
		Date lastSeenInvitation = acct.getLastSeenGroupInvitations();
		if (groupInvitationReceived == null)
			return false;
		if (lastSeenInvitation == null)
			return true;
		return groupInvitationReceived.compareTo(lastSeenInvitation) > 0;
	}
	
	/**
	 * Get a set of contacts of the viewed user that we want to display on the person page.
	 * 
	 * @return a list of PersonViews of a subset of contacts
	 */
	public ListBean<PersonView> getContacts() {
		if (contacts == null) {
			Set<PersonView> mingledContacts = 
				personViewer.getContacts(getSignin().getViewpoint(), getViewedUser(), 
						                   false, PersonViewExtra.INVITED_STATUS, 
						                   PersonViewExtra.PRIMARY_EMAIL, 
						                   PersonViewExtra.PRIMARY_AIM,
						                   PersonViewExtra.EXTERNAL_ACCOUNTS);		
			contacts = new ListBean<PersonView>(PersonView.sortedList(getSignin().getViewpoint(), getViewedUser(), mingledContacts));
			
			totalContacts = mingledContacts.size();
		}
		return contacts;
	}

	public Pageable<PersonView> getPageableContacts() {
        if (pageableContacts == null) {			
			pageableContacts = pagePositions.createPageable("friends"); 				
			pageableContacts.setInitialPerPage(FRIENDS_PER_PAGE);
			pageableContacts.setSubsequentPerPage(FRIENDS_PER_PAGE);
			
			pageableContacts.generatePageResults(getContacts().getList());
		}
		
		return pageableContacts;
	}
	
	public ListBean<PersonView> getFollowers() {
		if (followers == null) {
		    Set<PersonView> mingledFollowers = 
			    personViewer.getFollowers(getSignin().getViewpoint(), getViewedUser());		
		        followers = new ListBean<PersonView>(PersonView.sortedList(getSignin().getViewpoint(), getViewedUser(), mingledFollowers));
		}
		return followers;
	}

	public Pageable<PersonView> getPageableFollowers() {
        if (pageableFollowers == null) {			
        	pageableFollowers = pagePositions.createPageable("followers"); 				
        	pageableFollowers.setInitialPerPage(FRIENDS_PER_PAGE);
        	pageableFollowers.setSubsequentPerPage(FRIENDS_PER_PAGE);
			
			pageableFollowers.generatePageResults(getFollowers().getList());
		}
		
		return pageableFollowers;
	}
	
	public void setTotalContacts(int totalContacts) {		
	    this.totalContacts = totalContacts;
	}
	
	public int getTotalContacts() {
		if (contacts == null) {
			getContacts();
		}
		
		return totalContacts;
	}
	
	public TrackView getCurrentTrack() {
		if (!lookedUpCurrentTrack) {
			try {
			lookedUpCurrentTrack = true;
				currentTrack = getMusicSystem().getCurrentTrackView(getSignin().getViewpoint(), getViewedUser());
			} catch (NotFoundException e) {
			}
		}
		return currentTrack;
	}
	
	public ListBean<InvitationView> getOutstandingInvitations() {
		if (outstandingInvitations == null) {
			outstandingInvitations = 
				new ListBean<InvitationView>(
				    invitationSystem.findOutstandingInvitations(getUserSignin().getViewpoint(), 
				    		                                    0, -1));
		}
		return outstandingInvitations;
	}
	
	protected final boolean getNeedExternalAccounts() {
		return needExternalAccounts;
	}
	
	public void setNeedExternalAccounts(boolean needExternalAccounts) {
		this.needExternalAccounts = needExternalAccounts;
	}
}
