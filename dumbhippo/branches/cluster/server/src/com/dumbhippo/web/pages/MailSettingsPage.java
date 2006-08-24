package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.ToggleNoMailToken;
import com.dumbhippo.server.NoMailSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.web.FromJspContext;
import com.dumbhippo.web.Scope;
import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;
import com.dumbhippo.web.WebEJBUtil;

public class MailSettingsPage {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(MailSettingsPage.class);
	
	@FromJspContext(value="dumbhippo.toggleNoMailToken", scope=Scope.SESSION)
	private ToggleNoMailToken token;
	@Signin
	private UserSigninBean signin;
	
	private NoMailSystem noMail;
	
	private PersonView person;
	
	public MailSettingsPage() {
		noMail = WebEJBUtil.defaultLookup(NoMailSystem.class);
	}
	
	public SigninBean getSignin() { 
		return signin;
	}

	public PersonView getPerson() {
		if (person == null) {
			PersonViewer spider = WebEJBUtil.defaultLookup(PersonViewer.class);
			person = spider.getPersonView(signin.getViewpoint(), signin.getUser()); // don't get any extras
		}
		
		return person;
	}
	
	public String getEmail() {
		if (token == null)
			return null;
		else
			return token.getEmail().getEmail();
	}
	
	public boolean getEnabled() {
		return noMail.getMailEnabled(token.getEmail());
	}
	
	public String getEnableLink() {
		return noMail.getNoMailUrl(token, NoMailSystem.Action.WANTS_MAIL);
	}
	
	public String getDisableLink() {
		return noMail.getNoMailUrl(token, NoMailSystem.Action.NO_MAIL_PLEASE);
	}
}
