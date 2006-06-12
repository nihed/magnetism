package com.dumbhippo.web.pages;

import java.util.Set;

import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.web.ListBean;

/**
 * ContactsPage corresponds to contacts.jsp
 * 
 * @author marinaz
 * 
 */
public class ContactsPage extends AbstractPersonPage {
	static private final int ROWS_IN_A_GRID = 4;
	static private final int USERS_PER_ROW = 5;
	static private final int NON_USERS_PER_ROW = 2;
	
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
	
	/**
	 * Get a set of contacts of the viewed user that we want to display on the friends page,
	 * using start and stop fields as guidance.
	 * 
	 * @return a list of PersonViews of a subset of contacts
	 */
	@Override
	public ListBean<PersonView> getContacts() {
		if (contacts == null) {
			Set<PersonView> mingledContacts = 
				identitySpider.getContacts(getSignin().getViewpoint(), getViewedUser(), 
						                   false, PersonViewExtra.INVITED_STATUS, 
						                   PersonViewExtra.PRIMARY_EMAIL, 
						                   PersonViewExtra.PRIMARY_AIM);
			totalContacts = mingledContacts.size();
			
			if (stop > 0) {
				start = PersonView.computeStart(mingledContacts, 
						                        stop, 
						                        ROWS_IN_A_GRID,
						                        USERS_PER_ROW,
						                        NON_USERS_PER_ROW);
			} 
			    
			contacts = new ListBean<PersonView>(PersonView.sortedList(mingledContacts,
					                                                  start, 
					                                                  ROWS_IN_A_GRID,
					      						                      USERS_PER_ROW,
					      						                      NON_USERS_PER_ROW));		
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