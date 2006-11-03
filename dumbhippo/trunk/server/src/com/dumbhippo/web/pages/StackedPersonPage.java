package com.dumbhippo.web.pages;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.blocks.BlockView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.WebEJBUtil;

public class StackedPersonPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(StackedPersonPage.class);
	
	// We override the default values for initial and subsequent results per page from Pageable
	static private final int CONTACT_STACKS_PER_PAGE = 4;
	static private final int INITIAL_BLOCKS_PER_PAGE = 5;
	static private final int BLOCKS_PER_PAGE = 20;
	
	protected Stacker stacker;
	
	protected List<StackedContact> stackedContacts;
	
	private Pageable<StackedContact> pageableStackedContacts; 

	private Pageable<BlockView> pageableMugshot;
	private Pageable<BlockView> pageableStack;
	
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

	public List<BlockView> getMusghot() {
		if (getViewedUser() != null) {
			return stacker.getStack(getSignin().getViewpoint(), getViewedUser(), 0, 0, 20, true);
		}
		return null;
	}

	public Pageable<BlockView> getPageableMugshot() {
		if (pageableMugshot == null) {
			pageableMugshot = 
				pagePositions.createPageable("mugshot", INITIAL_BLOCKS_PER_PAGE); 
			pageableMugshot.setSubsequentPerPage(BLOCKS_PER_PAGE);
			pageableMugshot.setFlexibleResultCount(true);
			stacker.pageStack(getSignin().getViewpoint(), getViewedUser(), pageableMugshot, true);
		}

		return pageableMugshot;
	}
	
	public List<BlockView> getStack() {
		if (getViewedUser() != null) {
			return stacker.getStack(getSignin().getViewpoint(), getViewedUser(), 0, 0, 20);
		}
		return null;
	}
	
	public Pageable<BlockView> getPageableStack() {
		if (pageableStack == null) {
            if (getPageableMugshot().getPosition() == 0) {
			    pageableStack = pagePositions.createPageable("stacker", BLOCKS_PER_PAGE); 
            } else {
            	pageableStack = pagePositions.createBoundedPageable("stacker", INITIAL_BLOCKS_PER_PAGE, INITIAL_BLOCKS_PER_PAGE);
            }
			pageableStack.setSubsequentPerPage(BLOCKS_PER_PAGE);
			pageableStack.setFlexibleResultCount(true);
			stacker.pageStack(getSignin().getViewpoint(), getViewedUser(), pageableStack, false);
		}

		return pageableStack;
	}
	
	public List<BlockView> getFaveStack() {
		return null;
	}
	
	public ListBean<StackedContact> getContactStacks() {
		if (stackedContacts == null) {
			stackedContacts = new ArrayList<StackedContact>();
			for (PersonView pv : getUnsortedContacts()) {
				// we do not have stacks for non-user contacts, i.e. people you have invited or shared with,
				// these people can be viewed on one's friends page
				if (pv.getUser() != null) {
				    stackedContacts.add(new StackedContact(stacker.getStack(getSignin().getViewpoint(), pv.getUser(), 0, 0, 20), pv));
				}
			}		
			
			final Collator collator = Collator.getInstance();
			Collections.sort(stackedContacts, new Comparator<StackedContact>() {
				public int compare (StackedContact contact1, StackedContact contact2) {
					Long contact1ActivityTimestamp = Long.valueOf(0);
					if (!contact1.getStack().isEmpty())
						contact1ActivityTimestamp = contact1.getStack().get(0).getBlock().getTimestampAsLong();
					Long contact2ActivityTimestamp = Long.valueOf(0);
					if (!contact2.getStack().isEmpty())
						contact2ActivityTimestamp = contact2.getStack().get(0).getBlock().getTimestampAsLong();
	                // we want contacts with newer(greater) activity timestamps to be in the front of the list,
					// we also want contacts with same activity timestamps to be sorted alphabetically
					if (contact1ActivityTimestamp < contact2ActivityTimestamp) {
						return 1;
					} else if (contact1ActivityTimestamp > contact2ActivityTimestamp) {
						return -1;
				    } else {
				    	return collator.compare(contact1.getContact().getName(), contact2.getContact().getName());
				    }
				}
			});
		}
		
		return new ListBean<StackedContact>(stackedContacts);
	}
	
	public Pageable<StackedContact> getPageableContactStacks() {
        if (pageableStackedContacts == null) {			
        	pageableStackedContacts = pagePositions.createPageable("stackedContacts"); 				
        	pageableStackedContacts.setInitialPerPage(CONTACT_STACKS_PER_PAGE);
        	pageableStackedContacts.setSubsequentPerPage(CONTACT_STACKS_PER_PAGE);
			
        	pageableStackedContacts.generatePageResults(getContactStacks().getList());
		}
		
		return pageableStackedContacts;
	}
}
