package com.dumbhippo.web;

import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PostingBoard;

/**
 * Displays a list of posts from a person
 * 
 */

public class PersonPostView {
	static private final Log logger = GlobalSetup.getLog(PersonPostView.class);	
	
	private Person viewedPerson;
	private String viewedPersonId;
	
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;	
	
	public PersonPostView() throws NamingException {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}
	
	public List<String> getPostUrls() {
		assert viewedPerson != null;
		return postBoard.getPostedUrlsFor(viewedPerson, 0);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public void setSignin(SigninBean signin) {
		this.signin = signin;
	}

	public String getViewedPersonId() {
		return viewedPersonId;
	}
	
	protected void setViewedPerson(Person person) {
		this.viewedPerson = person;
		this.viewedPersonId = person.getId();
		logger.debug("viewing person: " + this.viewedPersonId);
	}

	public void setViewedPersonId(String viewedPersonId) throws ParseException {
		if (viewedPersonId == null && this.signin != null && this.signin.isValid()) {
			setViewedPerson(this.signin.getUser());
		} else if (viewedPersonId == null) {
			logger.debug("no viewed person");
			return;
		} else {
			setViewedPerson(identitySpider.lookupPersonById(new Guid(viewedPersonId)));
		}
	}
}
