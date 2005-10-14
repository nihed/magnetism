package com.dumbhippo.web;

import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.TestGlue;


public class AddClientBean {
	private String email;
		
	@Inject
	private transient TestGlue testGlue;

	public AddClientBean() {
		EjbLink.injectFromFacesContext(this, Scope.NONE);
	}
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public String doAddClient() {
		
		if (email == null) {
			/* FIXME need to complain on the form */
			return null;
		}
			
		HippoAccount account = testGlue.createAccountFromEmail(email);
		String authKey = testGlue.authorizeNewClient(account.getId(), SigninBean.computeClientIdentifier());
		SigninBean.setCookie(account.getOwner().getId(), authKey);

		return "main";
	}
}
