package com.dumbhippo.web;

import java.util.Set;

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
import com.dumbhippo.server.TrackView;

public abstract class AbstractPersonPage extends AbstractSigninOptionalPage {
	static private final Logger logger = GlobalSetup.getLogger(AbstractPersonPage.class);	
	
	static private final int MAX_CONTACTS_SHOWN = 9;
	
	private User viewedPerson;
	private String viewedPersonId;
	private boolean disabled;
	
	private GroupSystem groupSystem;
	private MusicSystem musicSystem;
	private PersonView person;
	
	private ListBean<Group> groups;
	
	private boolean lookedUpCurrentTrack;
	private TrackView currentTrack;
	
	protected int totalContacts;
	
	protected AbstractPersonPage() {	
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
		lookedUpCurrentTrack = false;
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
 	
	public String getViewedPersonId() {
		return viewedPersonId;
	}

	public User getViewedPerson() {
		return viewedPerson;
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
		
		if (identitySpider.getAccountDisabled(person)) {
				this.disabled = true;
		}
		
		logger.debug("viewing person: {} disabled = {}", this.viewedPerson, disabled);
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
				logger.debug("bad personId as person parameter {}", personId);
			} catch (NotFoundException e) {
				logger.debug("bad personId as person parameter {}", personId);
			}
		}
	}
	
	public PersonView getPerson() {
		if (person == null)
			person = identitySpider.getPersonView(getSignin().getViewpoint(), viewedPerson, PersonViewExtra.ALL_RESOURCES);
		
		return person;
	}
	
	public boolean isContact() {
		if (getSignin().isValid())
			return identitySpider.isContact(getSignin().getViewpoint(), getUserSignin().getUser(), viewedPerson);
		else
			return false;
	}
	
	public boolean isSelf() {
		if (getSignin().isValid() && viewedPerson != null) {
			return getUserSignin().getUser().equals(viewedPerson);
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
			groups = new ListBean<Group>(Group.sortedList(groupSystem.findRawGroups(getSignin().getViewpoint(), viewedPerson, MembershipStatus.ACTIVE)));
		}
		return groups;
	}
	
	public ListBean<PersonView> getContacts() {
		if (contacts == null) {
			Set<PersonView> mingledContacts = 
				identitySpider.getContacts(getSignin().getViewpoint(), viewedPerson, 
						                   false, PersonViewExtra.INVITED_STATUS, 
						                   PersonViewExtra.PRIMARY_EMAIL, 
						                   PersonViewExtra.PRIMARY_AIM);
			contacts = new ListBean<PersonView>(PersonView.sortedList(mingledContacts,
					                                                  1, MAX_CONTACTS_SHOWN, 
					                                                  1, 1));
			
			totalContacts = mingledContacts.size();
		}
		return contacts;
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
			lookedUpCurrentTrack = true;
			try {
				currentTrack = getMusicSystem().getCurrentTrackView(getSignin().getViewpoint(), getViewedUser());
			} catch (NotFoundException e) {
			}
		}
		return currentTrack;
	}
}
