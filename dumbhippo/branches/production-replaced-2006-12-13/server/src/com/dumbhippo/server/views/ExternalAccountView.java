package com.dumbhippo.server.views;

import com.dumbhippo.persistence.ExternalAccount;

public class ExternalAccountView {
    private ExternalAccount externalAccount;
    private String link;
    
    public ExternalAccountView(ExternalAccount externalAccount) {
    	this.externalAccount = externalAccount;
    }
    
    public ExternalAccountView(ExternalAccount externalAccount, String link) {
    	this.externalAccount = externalAccount;
    	this.link = link;
    }

	public ExternalAccount getExternalAccount() {
		return externalAccount;
	}

	public String getLink() {
		if (link != null)
		    return link;
		
		return externalAccount.getLink();
	}
}
