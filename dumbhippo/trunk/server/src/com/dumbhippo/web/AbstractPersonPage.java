package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;

public abstract class AbstractPersonPage {
	static private final Logger logger = GlobalSetup.getLogger(AbstractPersonPage.class);	
	
	private User viewedPerson;
	private String viewedPersonId;
	private boolean disabled;
	
	@Signin
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private GroupSystem groupSystem;
	private MusicSystem musicSystem;
	private PersonView person;
	
	private ListBean<Group> groups;
	private ListBean<PersonView> contacts;
	
	protected AbstractPersonPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
	}
	
	protected IdentitySpider getIdentitySpider() { 
		return identitySpider;
	}
	
	protected GroupSystem getGroupSystem() {
		return groupSystem;
	}
	
	protected MusicSystem getMusicSystem() {
		return musicSystem;
	}
 	
	public SigninBean getSignin() {
		return signin;
	}

	public String getViewedPersonId() {
		return viewedPersonId;
	}
	
	public User getViewedUser() {
		return viewedPerson;
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
	
	public ListBean<PersonView> getContacts() {
		if (contacts == null) {
			contacts = new ListBean<PersonView>(PersonView.sortedList(identitySpider.getContacts(signin.getViewpoint(), viewedPerson, false,
					PersonViewExtra.INVITED_STATUS, PersonViewExtra.PRIMARY_EMAIL, PersonViewExtra.PRIMARY_AIM)));
		}
		return contacts;
	}
}
