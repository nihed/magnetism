package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.Site;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;

@Local
public interface SigninSystem {

	public void sendSigninLinkEmail(Viewpoint viewpoint, String address) throws HumanVisibleException, RetryException;
	
	public String getSigninLinkAim(Viewpoint viewpoint, String address) throws HumanVisibleException, RetryException;
	
	public void sendRepairLink(Viewpoint viewpoint, User user) throws HumanVisibleException, RetryException; 
	
	public Client authenticatePassword(Site site, Guid guid, String password, String clientIdentifier) throws HumanVisibleException;
	
	public Client authenticatePassword(Site site, String address, String password, String clientIdentifier) throws HumanVisibleException;
	
	public void setPassword(User user, String password) throws HumanVisibleException;
}
