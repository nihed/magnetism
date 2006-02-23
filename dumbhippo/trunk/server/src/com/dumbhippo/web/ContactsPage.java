package com.dumbhippo.web;

import java.util.Set;

import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;

/**
 * ContactsPage corresponds to contacts.jsp
 * 
 * @author marinaz
 * 
 */
public class ContactsPage extends AbstractPersonPage {
	private int start;
	private int nextStart;
	private int stop;
	private boolean hasMoreContacts;

	protected ContactsPage() {
		start = 1;
		nextStart = 1;
		stop = -1;		
		hasMoreContacts = false;
	}
	
	public ListBean<PersonView> getContacts() {
		if (getViewedPersonId() == null) {
			setViewedPerson(signin.getUser());
		}
		if (contacts == null) {
			Set<PersonView> mingledContacts = 
				identitySpider.getContacts(signin.getViewpoint(), getViewedPerson(), 
						                   false, PersonViewExtra.INVITED_STATUS, 
						                   PersonViewExtra.PRIMARY_EMAIL, 
						                   PersonViewExtra.PRIMARY_AIM);
			
			if (stop > 0) {
				start = PersonView.computeStart(mingledContacts,stop, 4, 5, 2);
			} 
			    
			contacts = new ListBean<PersonView>(PersonView.sortedList(mingledContacts,
					                                                      start, 4, 5, 2));		
		    nextStart = start + contacts.getSize();
			if (nextStart <= mingledContacts.size()) {
				hasMoreContacts = true;
			}
			
		}
		return contacts;
	}
	
	public void setStart(int start) {
		if (start <= 0) {
			this.start = 1;
		} else {		
	        this.start = start;
		}
	}
	
	public int getStart() {
		return start;
	}

	public void setNextStart(int nextStart) {
		if (nextStart <= 0) {
			this.nextStart = 1;
		} else {		
	        this.nextStart = nextStart;
		}
	}
	
	public int getNextStart() {
		if (contacts == null) {
			getContacts();
		}
		
		return nextStart;
	}
	
	public void setStop(int stop) {	
	    this.stop = stop;
	}
	
	public int getStop() {
		return stop;
	}
	
	public void setHasMoreContacts(boolean hasMoreContacts) {
		this.hasMoreContacts = hasMoreContacts;
    }
	
	// isHasMoreContacts doesn't sound good	
	public boolean getHasMoreContacts() {
		if (contacts == null) {
			getContacts();
		}
		
		return hasMoreContacts;
	}
	
	
}
