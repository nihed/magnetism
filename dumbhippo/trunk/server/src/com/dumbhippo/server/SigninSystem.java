package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;

@Local
public interface SigninSystem {

	public void sendSigninLinkEmail(String address) throws HumanVisibleException, RetryException;
	
	public String getSigninLinkAim(String address) throws HumanVisibleException, RetryException;
	
	public void sendRepairLink(Viewpoint viewpoint, User user) throws HumanVisibleException, RetryException; 
	
	public Client authenticatePassword(String address, String password, String clientIdentifier) throws HumanVisibleException;
	
	public void setPassword(User user, String password) throws HumanVisibleException;
}
