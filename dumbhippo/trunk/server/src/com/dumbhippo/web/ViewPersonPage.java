package com.dumbhippo.web;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

/**
 * Displays a list of posts from a person
 * 
 */

public class ViewPersonPage {
	static private final Log logger = GlobalSetup.getLog(ViewPersonPage.class);	

	static private final int MAX_POSTS_SHOWN = 10;
	
	private User viewedPerson;
	private String viewedPersonId;
	private boolean disabled;
	
	@Signin
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private GroupSystem groupSystem;
	private PostingBoard postBoard;
	private PersonView person;
	
	private ListBean<PostView> posts;
	private ListBean<Group> groups;
	
	public ViewPersonPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
	}
	
	public ListBean<PostView> getPosts() {
		assert viewedPerson != null;
	
		if (posts == null) {
			// always ask for max posts shown + 1 as a marker for whether to show the More link
			posts = new ListBean<PostView>(postBoard.getPostsFor(signin.getViewpoint(), viewedPerson, 0, MAX_POSTS_SHOWN + 1));
		}
		return posts;
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public String getViewedPersonId() {
		return viewedPersonId;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	
	protected void setViewedPerson(User person) {
		this.viewedPerson = person;
		this.viewedPersonId = person.getId();
		
		if (person instanceof User &&
				identitySpider.getAccountDisabled((User) person)) {
				this.disabled = true;
		}
		
		logger.debug("viewing person: " + this.viewedPerson + " disabled = " + disabled);
	}
	
	public String getName() {
		return viewedPerson.getNickname();
	}

	public void setViewedPersonId(String personId) {
		if (personId == null) {
			logger.debug("no viewed person");
			return;
		} else {
			try {
				setViewedPerson(identitySpider.lookupGuidString(User.class, personId));
			} catch (ParseException e) {
				logger.debug("bad personId as person parameter " + personId);
			} catch (NotFoundException e) {
				logger.debug("bad personId as person parameter " + personId);
			}
		}
	}
	
	public PersonView getPerson() {
		if (person == null)
			person = identitySpider.getPersonView(signin.getViewpoint(), viewedPerson, PersonViewExtra.ALL_RESOURCES);
		
		return person;
	}
	
	public boolean isContact() {
		if (signin.isValid())
			return identitySpider.isContact(signin.getViewpoint(), signin.getUser(), viewedPerson);
		else
			return false;
	}
	
	public boolean isSelf() {
		if (signin.isValid() && viewedPerson != null) {
			return signin.getUser().equals(viewedPerson);
		} else {
			return false;
		}
	}
	
	public boolean isValid() {
		return viewedPerson != null;
	}
	
	// We don't show group's you haven't accepted the invitation for on your public page
	public ListBean<Group> getGroups() {
		if (groups == null) {
			groups = new ListBean<Group>(Group.sortedList(groupSystem.findRawGroups(signin.getViewpoint(), viewedPerson, MembershipStatus.ACTIVE)));
		}
		return groups;
	}
	
	public int getMaxPostsShown() {
		return MAX_POSTS_SHOWN;
	}
}
