package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

/**
 * backing bean for /person
 * 
 */

public class PersonPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PersonPage.class);	
	
	private boolean asOthersWouldSee;

	public PersonPage() {
	}
	
	public boolean isAsOthersWouldSee() {
		return asOthersWouldSee;
	}

	public void setAsOthersWouldSee(boolean asOthersWouldSee) {
		this.asOthersWouldSee = asOthersWouldSee;
	}
}
