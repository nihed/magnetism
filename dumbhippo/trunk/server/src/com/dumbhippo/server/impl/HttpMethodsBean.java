package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostingBoard;

@Stateless
public class HttpMethodsBean implements HttpMethods, Serializable {
	
	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(HttpMethodsBean.class);
	
	private static final long serialVersionUID = 0L;

	@EJB
	private AccountSystem accountSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PostingBoard postingBoard;

	/* (non-Javadoc)
	 * @see com.dumbhippo.server.AjaxGlueHttp#getFriendCompletions(java.io.OutputStream, java.lang.String, java.lang.String)
	 */
	public void getFriendCompletions(OutputStream out, HttpResponseData contentType, String entryContents) throws IOException {

		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");
		
		XmlBuilder xml = new XmlBuilder();

		xml.appendStandaloneFragmentHeader();
		
		xml.append("<people>");
		
		if (entryContents != null) {
			Set<HippoAccount> accounts = accountSystem.getActiveAccounts();
			for (HippoAccount a : accounts) {
				// FIXME get from viewpoint of personId

				// it's important that empty string returns all completions, otherwise
				// the arrow on the combobox doesn't drop down anything when it's empty
				
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
	
	public void doShareLink(Person user, String url, String recipientIds, String description) throws ParseException {
		Set<String> recipientGuids;
		
		// string.split returns a single empty string if the string we split is length 0, unfortunately
		if (recipientIds.length() > 0) {
			recipientGuids = new HashSet<String>(Arrays.asList(recipientIds.split(",")));
		} else {
			recipientGuids = Collections.emptySet();
		}
		postingBoard.createURLPost(user, null, description, url, recipientGuids);
	}
}
