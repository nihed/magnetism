package com.dumbhippo.web;

import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.PromotionCode;

/**
 * @author otaylor
 *
 * Backing bean for the /download page.
 *
 * Formerly used for more; so has various informatino for the logged in user, such as 
 * links recently shared with him.
 */
public class DownloadPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DownloadPage.class);
	static private final int MAX_RECEIVED_POSTS_SHOWN = 6;
	
	@Signin
	private UserSigninBean signin;
	
	@Browser
	private BrowserBean browser;
	
	private Configuration configuration;
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private PersonView person;
	private GroupSystem groupSystem;
	private InvitationSystem invitationSystem;
	
	private ListBean<PostView> receivedPosts;
	private ListBean<GroupView> groups;
	private InvitationToken creatingInvitation;
	
	public DownloadPage() {
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public BrowserBean getBrowser() {
		return browser;
	}
	
	public PersonView getPerson() {
		if (person == null)
			person = identitySpider.getPersonView(signin.getViewpoint(), signin.getUser());
		
		return person;
	}
	
	public ListBean<PostView> getReceivedPosts() {
		if (receivedPosts == null)
			receivedPosts = new ListBean<PostView>(postBoard.getReceivedPosts(signin.getViewpoint(), signin.getUser(), 0, MAX_RECEIVED_POSTS_SHOWN + 1));
		return receivedPosts;
	}
	
	public ListBean<GroupView> getGroups() {
		if (groups == null)
			groups = new ListBean<GroupView>(GroupView.sortedList(groupSystem.findGroups(signin.getViewpoint(), signin.getUser())));
		return groups;
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getProperty(HippoProperty.DOWNLOADURL_WINDOWS);
	}
	
	public List<PersonView> getInviters() {
		return PersonView.sortedList(invitationSystem.findInviters(signin.getViewpoint()));
	}
	
	public String getFeedbackEmail() {
		return configuration.getProperty(HippoProperty.FEEDBACK_EMAIL);
	}
	
	public int getMaxReceivedPostsShown() {
		return MAX_RECEIVED_POSTS_SHOWN;
	}
	
	private InvitationToken getCreatingInvitation() {
		if (creatingInvitation == null)
			creatingInvitation = invitationSystem.getCreatingInvitation(getPerson().getAccount());
		
		return creatingInvitation;
	}
	
	public boolean isFromMySpace() {
		InvitationToken invite = getCreatingInvitation();
		if (invite == null)
			return false;
		PromotionCode promotionCode = invite.getPromotionCode();
		if (promotionCode == null)
			return false;
		
		return promotionCode.isFromMySpace(); 
	}
}
