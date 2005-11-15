package com.dumbhippo.web;

import java.util.List;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

/**
 * @author otaylor
 *
 * Displays information for the logged in user, such as links recently
 * shared with him.
 */
public class HomePage {
	static private final Log logger = GlobalSetup.getLog(HomePage.class);

	@Signin
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private PersonView person;
	private GroupSystem groupSystem;
	
	public HomePage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
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
		return postBoard.getReceivedPosts(signin.getViewpoint(), signin.getUser(), 4);
	}
	
	public List<GroupView> getGroups() {
		return GroupView.sortedList(groupSystem.findGroups(signin.getViewpoint(), signin.getUser()));
	}
	
	public List<PersonView> getContacts() {
		return PersonView.sortedList(identitySpider.getContacts(signin.getViewpoint(), signin.getUser()));
	}
	
	public List<PostView> getContactPosts() {
		return postBoard.getContactPosts(signin.getViewpoint(), signin.getUser(), false, 0);
	}
}
