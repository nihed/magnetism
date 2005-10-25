package com.dumbhippo.web;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonInfo;
import com.dumbhippo.server.PostInfo;
import com.dumbhippo.server.PostingBoard;

/**
 * @author otaylor
 *
 * Displays information for the logged in user, such as links recently
 * shared with him.
 */
public class HomePage {
	static private final Log logger = GlobalSetup.getLog(HomePage.class);
	
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private PersonInfo personInfo;
	private GroupSystem groupSystem;
	
	public HomePage() throws NamingException {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
	}
	
	public List<PostInfo> getReceivedPostInfos() {
		logger.debug("Getting received posts for " + signin.getUser().getId());
		return postBoard.getReceivedPostInfos(signin.getUser(), 0);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public void setSignin(SigninBean signin) {
		this.signin = signin;
	}

	public PersonInfo getPersonInfo() {
		if (personInfo == null)
			personInfo = new PersonInfo(identitySpider, signin.getUser(), signin.getUser());
		
		return personInfo;
	}
	
	public List<Group> getGroups() {
		// Sort the return of groupSystem.findGroups(), so we don't
		// display things to the user in hash-table order.
		
		ArrayList<Group> groups = new ArrayList<Group>();
		groups.addAll(groupSystem.findGroups(signin.getUser()));
		
		final Collator collator = Collator.getInstance();
		Collections.sort(groups, new Comparator<Group>() {
			public int compare (Group g1, Group g2) {
				return collator.compare(g1.getName(), g2.getName());
			}
		});

		return groups;
	}
	
	public List<PersonInfo> getContacts() {
		ArrayList<PersonInfo> contacts = new ArrayList<PersonInfo>();
		contacts.addAll(identitySpider.getContactInfos(signin.getUser(), signin.getUser()));
		
		final Collator collator = Collator.getInstance();
		Collections.sort(contacts, new Comparator<PersonInfo>() {
			public int compare (PersonInfo i1, PersonInfo i2) {
				return collator.compare(i1.getHumanReadableName(), i2.getHumanReadableName());
			}
		});

		return contacts;
	}
}
