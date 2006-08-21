package com.dumbhippo.web.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.WebEJBUtil;

/**
 * backing bean for /person
 * 
 */

public class PersonPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PersonPage.class);	
	
	private boolean asOthersWouldSee;
	private ListBean<ExternalAccount> lovedAccounts;
	private ListBean<ExternalAccount> hatedAccounts;
	
	private Configuration config;
	
	public PersonPage() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
	}
	
	public boolean isAsOthersWouldSee() {
		return asOthersWouldSee;
	}

	public void setAsOthersWouldSee(boolean asOthersWouldSee) {
		this.asOthersWouldSee = asOthersWouldSee;
	}
	
	private ListBean<ExternalAccount> getAccountsBySentiment(Sentiment sentiment) {
		List<ExternalAccount> list = new ArrayList<ExternalAccount>();
		PersonView pv = getViewedPerson();
		Set<ExternalAccount> accounts = pv.getExternalAccounts();
		for (ExternalAccount a : accounts) {
			if (a.getSentiment() == sentiment) {
				list.add(a);
			}
		}
		Collections.sort(list, new Comparator<ExternalAccount>() {

			public int compare(ExternalAccount first, ExternalAccount second) {
				// Equality should be impossible, someone should not have two of the same account.
				// But we'll put it here in case the java sort algorithm somehow needs it (tough to imagine)
				if (first.getAccountType() == second.getAccountType())
					return 0;
				
				// We want "my website" first, then everything alphabetized by the human-readable name.
				
				if (first.getAccountType() == ExternalAccountType.WEBSITE)
					return -1;
				if (second.getAccountType() == ExternalAccountType.WEBSITE)
					return 1;
				
				return String.CASE_INSENSITIVE_ORDER.compare(first.getSiteName(), second.getSiteName());
			}
			
		});
		return new ListBean<ExternalAccount>(list);
	}
	
	public ListBean<ExternalAccount> getLovedAccounts() {
		if (lovedAccounts == null) {
			lovedAccounts = getAccountsBySentiment(Sentiment.LOVE);
		}
		return lovedAccounts;
	}
	
	public ListBean<ExternalAccount> getHatedAccounts() {
		if (hatedAccounts == null) {
			hatedAccounts = getAccountsBySentiment(Sentiment.HATE);
		}
		return hatedAccounts;
	}

	public String getAimPresenceImageLink() {
		PersonView pv = getViewedPerson();
		AimResource aim = pv.getAim();
		if (aim == null)
			return null;
		String aimName = aim.getScreenName();
		
		String presenceKey;
		try {
			 presenceKey = config.getPropertyNoDefault(HippoProperty.AIM_PRESENCE_KEY);
		} catch (PropertyNotFoundException pnfe) {
			return null;
		}
		if (presenceKey.length() == 0)
			return null;

		return "http://api.oscar.aol.com/SOA/key=" + presenceKey + "/presence/" + aimName;
	}
	
	public String getAimLink() {
		PersonView pv = getViewedPerson();
		AimResource aim = pv.getAim();
		if (aim == null)
			return null;
		String aimName = aim.getScreenName();
		return "aim:GoIM?screenname=" + StringUtils.urlEncode(aimName);
	}
	
	public String getEmailLink() {
		PersonView pv = getViewedPerson();
		EmailResource email = pv.getEmail();
		if (email == null)
			return null;
		return "mailto:" + StringUtils.urlEncodeEmail(email.getEmail());
	}
	
	private String getFullProfileUrl() {
		return config.getBaseUrl().toExternalForm() + "/person?who=" + getViewedUserId();
	}
	
	// this is done in Java instead of in the jsp because the escaping is too mind-melting otherwise
	public String getWhereImAtHtml() {
		XmlBuilder xml = new XmlBuilder();
		if (getViewedUser() == null) {
			xml.openElement("div", "class", "mugshot-error");
			xml.append("The script url should have ?who=userId where userId matches your profile page url");
			xml.closeElement();			
		} else if (isDisabled()) {
			xml.openElement("div", "class", "mugshot-error");
			xml.appendTextNode("a", "This account is disabled - no web 2.0 for you!",
					"href", getFullProfileUrl());
			xml.closeElement();						
		} else {
			List<ExternalAccount> loved = getLovedAccounts().getList();
			if (loved.size() == 0) {
				xml.openElement("div", "class", "mugshot-error");
				xml.appendTextNode("a", "Where's the love?", "href",
						getFullProfileUrl());
				xml.closeElement();
			} else {
				xml.openElement("ul", "class", "mugshot-external-accounts");
				xml.append("\n");
				for (ExternalAccount a : loved) {
					xml.openElement("li", "class", "mugshot-external-account");
					xml.appendTextNode("a", a.getSiteName(), "href", a.getLink());
					xml.append("\n");
				}
				xml.closeElement();
			}
		}
		xml.append("\n");
		return xml.toString();
	}
}
