package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.User;

@Local
public interface SigninSystem {

	public void sendSigninLink(String address) throws HumanVisibleException;
	
	public Pair<Client,User> authenticatePassword(String address, String password, String clientIdentifier) throws HumanVisibleException;
	
	public void setPassword(User user, String password) throws HumanVisibleException;
}
