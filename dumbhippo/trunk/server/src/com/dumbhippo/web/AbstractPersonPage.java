package com.dumbhippo.web;

import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;

public abstract class AbstractPersonPage extends AbstractSigninOptionalPage {
	static private final Logger logger = GlobalSetup.getLogger(AbstractPersonPage.class);	
	
	static private final int MAX_CONTACTS_SHOWN = 9;
	
	private User viewedUser;
	private String viewedUserId;
	private boolean disabled;
	
	private GroupSystem groupSystem;
	private MusicSystem musicSystem;
	private PersonView viewedPerson;
	
	private ListBean<GroupView> groups;
	
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
		if (viewedPerson == null)
			viewedPerson = identitySpider.getPersonView(getSignin().getViewpoint(), getViewedUser(), PersonViewExtra.ALL_RESOURCES);
		
		return viewedPerson;
	}
	
	public boolean isContact() {
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
		Viewpoint viewpoint = getSignin().getViewpoint();
		// For now we only display groups for users from non-anonymous viewpoints
		if (groups == null && viewpoint instanceof UserViewpoint) {
			groups = new ListBean<GroupView>(GroupView.sortedList(groupSystem.findGroups((UserViewpoint) viewpoint, getViewedUser())));
		}
		return groups;
	}	
	
	/**
	 * Get a set of contacts of the viewed user that we want to display on the person page.
	 * 
	 * @return a list of PersonViews of a subset of contacts
	 */
	public ListBean<PersonView> getContacts() {
		if (contacts == null) {
			Set<PersonView> mingledContacts = 
				identitySpider.getContacts(getSignin().getViewpoint(), getViewedUser(), 
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
