package com.dumbhippo.server.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AbstractLoginRequired;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.AjaxGlueXmlRpc;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;

@Stateful
public class AjaxGlueBean extends AbstractLoginRequired implements AjaxGlueXmlRpc, Serializable {
	
	private static final long serialVersionUID = 0L;
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	@EJB
	private transient AccountSystem accountSystem;
	
	@EJB
	private transient IdentitySpider identitySpider;
	
	public String getStuff() {
		return "This is some stuff!";
	}

	public List<String> getFriendCompletions(String entryContents) {
		List<String> completions = new ArrayList<String>();
		
		Set<HippoAccount> accounts = accountSystem.getActiveAccounts();
		for (HippoAccount a : accounts) {
			// FIXME get from viewpoint of personId
			
			String completion = null;
			
			PersonView view = identitySpider.getSystemViewpoint(a.getOwner());
			String humanReadable = view.getHumanReadableName();
			EmailResource email = view.getEmail();
			if (humanReadable.startsWith(entryContents)) {
				completion = humanReadable;
			} else if (email.getEmail().startsWith(entryContents)) {
				completion = email.getEmail();
			} else if (a.getOwner().getId().startsWith(entryContents)) {
				completion = a.getOwner().getId();
			}
			
			if (completion != null) {
				completions.add(completion);
			}
		}
		
		Collections.sort(completions);
		
		// we want the currently-typed string at the top
		if (entryContents != null)
			completions.add(0, entryContents);
		
		return completions;
	}
}
