package com.dumbhippo.web;

import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonInfo;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.PostInfo;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

/**
 * Displays a list of posts from a person
 * 
 */

public class ViewPersonPage {
	static private final Log logger = GlobalSetup.getLog(ViewPersonPage.class);	
	
	private Person viewedPerson;
	private String viewedPersonId;
	
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private PersonInfo personInfo;
	
	public ViewPersonPage() throws NamingException {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}
	
	public List<PostInfo> getPostInfos() {
		assert viewedPerson != null;
		return postBoard.getPostInfosFor(viewedPerson, signin.getUser(), 0);
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
	
	public String getName() {
		return viewedPerson.getName().toString();
	}

	public void setViewedPersonId(String viewedPersonId) throws ParseException, GuidNotFoundException {
		if (viewedPersonId == null && this.signin != null && this.signin.isValid()) {
			setViewedPerson(this.signin.getUser());
		} else if (viewedPersonId == null) {
			logger.debug("no viewed person");
			return;
		} else {
			setViewedPerson(identitySpider.lookupGuidString(Person.class, viewedPersonId));
		}
	}
	
	public PersonInfo getPersonInfo() {
		if (personInfo == null)
			personInfo = new PersonInfo(identitySpider, signin.getUser(), viewedPerson);
		
		return personInfo;
	}
}
