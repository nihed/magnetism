package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.ToggleNoMailToken;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.tx.RetryException;

@Local
public interface NoMailSystem {

	enum Action { WANTS_MAIL, NO_MAIL_PLEASE, TOGGLE_MAIL };
	
	public boolean getMailEnabled(EmailResource email);
	
	public void processAction(EmailResource email, Action action) throws RetryException;
	
	public String getNoMailUrl(String address, Action action) throws ValidationException, RetryException;
	public String getNoMailUrl(ToggleNoMailToken token, Action action);
	public String getNoMailUrl(EmailResource email, Action action) throws RetryException;
}
