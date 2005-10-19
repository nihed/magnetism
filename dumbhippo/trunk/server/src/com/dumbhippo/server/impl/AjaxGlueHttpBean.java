package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AbstractLoginRequired;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.AjaxGlueHttp;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;

@Stateful
public class AjaxGlueHttpBean extends AbstractLoginRequired implements AjaxGlueHttp, Serializable {
	
	private static final long serialVersionUID = 0L;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private AccountSystem accountSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	public void getFriendCompletions(OutputStream out, String contentType, String entryContents) throws IOException {
		XmlBuilder xml = new XmlBuilder();

		xml.appendStandaloneFragmentHeader();
		
		xml.append("<people>");
		
		if (entryContents != null && entryContents.length() > 0) {
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
					xml.appendElement("person", null, "id", a.getOwner().getId(), "display", humanReadable, "completion", completion);
				}
			}
		}

		xml.append("</people>");
		
		out.write(xml.toString().getBytes());
	}
}
