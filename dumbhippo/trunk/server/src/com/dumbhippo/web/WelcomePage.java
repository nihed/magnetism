package com.dumbhippo.web;

import java.util.List;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

/**
 * @author otaylor
 *
 * Displays information for the logged in user, such as links recently
 * shared with him.
 */
public class WelcomePage {
	static private final Log logger = GlobalSetup.getLog(WelcomePage.class);

	@Signin
	private SigninBean signin;
	
	private Configuration configuration;
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private PersonView person;
	private GroupSystem groupSystem;
	private InvitationSystem invitationSystem;
	
	public WelcomePage() {
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public PersonView getPerson() {
		if (person == null)
			person = identitySpider.getPersonView(signin.getViewpoint(), signin.getUser());
		
		return person;
	}
	
	public List<PostView> getReceivedPosts() {
		logger.debug("Getting received posts for " + signin.getUser().getId());
		return postBoard.getReceivedPosts(signin.getViewpoint(), signin.getUser(), 0, 0);
	}
	
	public List<GroupView> getGroups() {
		return GroupView.sortedList(groupSystem.findGroups(signin.getViewpoint(), signin.getUser()));
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getProperty(HippoProperty.DOWNLOADURL_WINDOWS);
	}
	
	public List<PersonView> getInviters() {
		return PersonView.sortedList(invitationSystem.findInviters(signin.getUser()));
	}
}
