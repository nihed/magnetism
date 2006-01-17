package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

/**
 * @author otaylor
 *
 * Displays information for the logged in user, such as links recently
 * shared with him.
 */
public class HomePage {
	static private final Logger logger = GlobalSetup.getLogger(HomePage.class);
	static private final int MAX_RECEIVED_POSTS_SHOWN = 4;
	
	@Signin
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private PersonView person;
	private GroupSystem groupSystem;
	private InvitationSystem invitationSystem;

	private ListBean<PostView> receivedPosts;
	private ListBean<PostView> contactPosts;
	private ListBean<GroupView> groups;
	private ListBean<PersonView> contacts;	
	private int invitations;


	public HomePage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		invitations = -1;
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public PersonView getPerson() {
		if (person == null)
			person = identitySpider.getPersonView(signin.getViewpoint(), signin.getUser(), PersonViewExtra.ALL_RESOURCES);
		
		return person;
	}
	
	public ListBean<PostView> getReceivedPosts() {
		if (receivedPosts == null) {
			logger.debug("Getting received posts for " + signin.getUser().getId());
			// + 1 as a marker for whether there are more
			receivedPosts = new ListBean<PostView>(postBoard.getReceivedPosts(signin.getViewpoint(), signin.getUser(), 0, MAX_RECEIVED_POSTS_SHOWN + 1));
		}
		return receivedPosts;
	}
	
	public ListBean<GroupView> getGroups() {
		if (groups == null) {
			groups = new ListBean<GroupView>(GroupView.sortedList(groupSystem.findGroups(signin.getViewpoint(), signin.getUser())));
		}
		return groups;
	}
	
	public ListBean<PersonView> getContacts() {
		if (contacts == null) {
			contacts = new ListBean<PersonView>(PersonView.sortedList(identitySpider.getContacts(signin.getViewpoint(), signin.getUser(), false, PersonViewExtra.INVITED_STATUS, PersonViewExtra.PRIMARY_EMAIL, PersonViewExtra.PRIMARY_AIM)));
		}
		return contacts;
	}
	
	public ListBean<PostView> getContactPosts() {
		if (contactPosts == null) {
			contactPosts = new ListBean<PostView>(postBoard.getContactPosts(signin.getViewpoint(), signin.getUser(), false, 0, 0));
		}
		return contactPosts;
	}
	
	public int getMaxReceivedPostsShown() {
		return MAX_RECEIVED_POSTS_SHOWN;
	}
	
	public int getInvitations() {
		if (invitations < 0) {
			invitations = invitationSystem.getInvitations(signin.getUser()); 
		}
		return invitations;
	}
}
