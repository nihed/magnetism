package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.User;

@Local
public interface SigninSystem {

	public void sendSigninLinkEmail(String address) throws HumanVisibleException;
	
	public String getSigninLinkAim(String address) throws HumanVisibleException;
	
	public void sendRepairLink(User user) throws HumanVisibleException; 
	
	public Client authenticatePassword(String address, String password, String clientIdentifier) throws HumanVisibleException;
	
	public void setPassword(User user, String password) throws HumanVisibleException;
}
