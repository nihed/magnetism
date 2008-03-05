package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.EmailDetails;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;

/*
 * Grab-bag class for controls relating only to Online Desktop.
 */
@Local
public interface OnlineDesktopSystem {

	public EmailDetails lookupEmailDetails(EmailResource email);
	
	public List<EmailResource> getGoogleEnabledEmails(Viewpoint viewpoint, User user);
	
	void setGoogleServicedEmail(Viewpoint viewpoint, User user, EmailResource email, boolean enabled) throws RetryException;

    public void onGoogleServicedEmailChange(Viewpoint viewpoint, User user, EmailResource email);
}
