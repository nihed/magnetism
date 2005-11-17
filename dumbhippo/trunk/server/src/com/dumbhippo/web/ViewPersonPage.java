package com.dumbhippo.web;

import java.util.List;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

/**
 * Displays a list of posts from a person
 * 
 */

public class ViewPersonPage {
	static private final Log logger = GlobalSetup.getLog(ViewPersonPage.class);	
	
	private Person viewedPerson;
	private String viewedPersonId;

	@Signin
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private GroupSystem groupSystem;
	private PostingBoard postBoard;
	private PersonView person;
	
	public ViewPersonPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
	}
	
	public List<PostView> getPosts() {
		assert viewedPerson != null;
		return postBoard.getPostsFor(signin.getViewpoint(), viewedPerson, 10);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public String getViewedPersonId() {
		return viewedPersonId;
	}
	
	protected void setViewedPerson(Person person) {
		this.viewedPerson = person;
		this.viewedPersonId = person.getId();
		logger.debug("viewing person: " + this.viewedPersonId);
	}
	
	public String getName() {
		return viewedPerson.getName().toString();
	}

	public void setViewedPersonId(String personId) throws ParseException, GuidNotFoundException {
		if (personId == null) {
			logger.debug("no viewed person");
			return;
		} else {
			setViewedPerson(identitySpider.lookupGuidString(Person.class, personId));
		}
	}
	
	public PersonView getPerson() {
		if (person == null)
			person = identitySpider.getPersonView(signin.getViewpoint(), viewedPerson);
		
		return person;
	}
	
	public boolean getIsContact() {
		if (signin.isValid())
			return identitySpider.isContact(signin.getViewpoint(), signin.getUser(), viewedPerson);
		else
			return false;
	}
	
	// We don't show group's you haven't accepted the invitation for on your public page
	public List<Group> getGroups() {
		return Group.sortedList(groupSystem.findRawGroups(signin.getViewpoint(), viewedPerson, MembershipStatus.ACTIVE));
	}
}
