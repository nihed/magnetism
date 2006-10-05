package com.dumbhippo.web.pages;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.views.BlockView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.WebEJBUtil;

public class StackedPersonPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(StackedPersonPage.class);
	
	protected Stacker stacker;
	
	protected List<StackedContact> stackedContacts;
	
	public static class StackedContact {
		private List<BlockView> stack;
		private PersonView contact;
		
		public StackedContact(List<BlockView> stack, PersonView contact) {
			this.stack = stack;
			this.contact = contact;
		}
		public PersonView getContact() {
			return contact;
		}
		public List<BlockView> getStack() {
			return stack;
		}
	}
	
	public StackedPersonPage() {
		stacker = WebEJBUtil.defaultLookup(Stacker.class);
	}
	
	public List<BlockView> getStack() {
		if (getViewedUser() != null) {
			return stacker.getStack(getSignin().getViewpoint(), getViewedUser(), 0, 0, 20);
		}
		return null;
	}
	
	public ListBean<StackedContact> getContactStacks() {
		if (stackedContacts == null) {
			stackedContacts = new ArrayList<StackedContact>();
			for (PersonView pv : getContacts().getList()) {
				// we do not have stacks for non-user contacts, i.e. people you have invited or shared with,
				// these people can be viewed on one's friends page
				if (pv.getUser() != null) {
				    stackedContacts.add(new StackedContact(stacker.getStack(getSignin().getViewpoint(), pv.getUser(), 0, 0, 5), pv));
				}
			}				
		}
		return new ListBean<StackedContact>(stackedContacts);
	}
}
