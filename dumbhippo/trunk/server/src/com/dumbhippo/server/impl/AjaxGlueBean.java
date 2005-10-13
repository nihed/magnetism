package com.dumbhippo.server.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.AjaxGlue;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;

@Stateful
public class AjaxGlueBean implements AjaxGlue, Serializable {
	
	private static final long serialVersionUID = 0L;
	
	private String personId = null;
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	@EJB
	private transient AccountSystem accountSystem;
	
	@EJB
	private transient IdentitySpider identitySpider;
	
	public void init(String personId) {
		this.personId = personId;
	}
	
	public String getStuff() {
		return "This is some stuff!";
	}

	public List<String> getFriendCompletions(String entryContents) {
		List<String> completions = new ArrayList<String>();
		if (entryContents != null)
			completions.add(entryContents);
		Set<HippoAccount> accounts = accountSystem.getActiveAccounts();
		for (HippoAccount a : accounts) {
			// FIXME get from viewpoint of personId
			PersonView view = identitySpider.getSystemViewpoint(a.getOwner());
			completions.add(view.getHumanReadableName());
		}
		
		completions.add("Test Person 1");
		completions.add("Test Person 2");
		
		return completions;
	}
}
