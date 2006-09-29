package com.dumbhippo.web.pages;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.WebBlock;
import com.dumbhippo.web.WebEJBUtil;

public class StackedPersonPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(StackedPersonPage.class);
	
	private Stacker stacker;
	
	private List<StackedContact> stackedContacts;
	
	public static class StackedContact {
		private List<WebBlock> stack;
		private PersonView contact;
		
		public StackedContact(List<WebBlock> stack, PersonView contact) {
			this.stack = stack;
			this.contact = contact;
		}
		public PersonView getContact() {
			return contact;
		}
		public List<WebBlock> getStack() {
			return stack;
		}
	}
	
	public StackedPersonPage() {
		stacker = WebEJBUtil.defaultLookup(Stacker.class);
	}
	
	private List<WebBlock> convertBlocks(List<UserBlockData> userBlocks) {
		List<WebBlock> blocks = new ArrayList<WebBlock>();				
		for (UserBlockData ubd : userBlocks) {
			blocks.add(new WebBlock(ubd.getBlock()));
		}		
		return blocks;
	}
	
	public List<WebBlock> getStack() {
		if (getViewedUser() != null) {
			return convertBlocks(stacker.getStack(getSignin().getViewpoint(), getViewedUser(), 0, 0, 5));
		}
		return null;
	}
	
	public ListBean<StackedContact> getContactStacks() {
		if (stackedContacts == null) {
			stackedContacts = new ArrayList<StackedContact>();
			for (PersonView pv : getContacts().getList()) {
				stackedContacts.add(new StackedContact(convertBlocks(stacker.getStack(getSignin().getViewpoint(), pv.getUser(), 0, 0, 5)), pv));
			}				
		}
		return new ListBean<StackedContact>(stackedContacts);
	}
}
