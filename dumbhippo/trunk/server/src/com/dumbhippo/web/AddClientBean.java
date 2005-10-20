package com.dumbhippo.web;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.TestGlue;


public class AddClientBean {
	
	static private final Log logger = GlobalSetup.getLog(AddClientBean.class);
	
	private String email;
	
	private String goBackTo;
	
	@Inject
	private TestGlue testGlue;

	public AddClientBean() {
		EjbLink.injectFromFacesContext(this, Scope.NONE);
	}
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getGoBackTo() {
		return goBackTo;
	}

	public void setGoBackTo(String goBackTo) {
		logger.debug("goBackTo = " + goBackTo);
		this.goBackTo = goBackTo;
	}
	
	public String doAddClient() {

		logger.debug("doAddClient()");
		
		if (email == null) {
			logger.debug("no email filled in");
			return null;
		}
			
		HippoAccount account = testGlue.findOrCreateAccountFromEmail(email);
		String authKey = testGlue.authorizeNewClient(account.getId(), SigninBean.computeClientIdentifier());
		SigninBean.setCookie(account.getOwner().getId(), authKey);

		if (goBackTo != null) {
			logger.debug("sending back to " + goBackTo);
			return goBackTo;
		} else {
			logger.debug("sending to main");
			return "main";
		}
	}
}
