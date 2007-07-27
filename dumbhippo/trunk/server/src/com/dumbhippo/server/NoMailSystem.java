package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.ToggleNoMailToken;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;

@Local
public interface NoMailSystem {

	enum Action { WANTS_MAIL, NO_MAIL_PLEASE, TOGGLE_MAIL };
	
	public boolean getMailEnabled(EmailResource email);
	
	public void processAction(EmailResource email, Action action) throws RetryException;
	
	public String getNoMailUrl(Viewpoint viewpoint, String address, Action action) throws ValidationException, RetryException;
	public String getNoMailUrl(Viewpoint viewpoint, ToggleNoMailToken token, Action action);
	public String getNoMailUrl(Viewpoint viewpoint, EmailResource email, Action action) throws RetryException;
}
