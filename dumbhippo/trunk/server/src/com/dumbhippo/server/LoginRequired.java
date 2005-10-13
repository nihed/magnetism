package com.dumbhippo.server;

import javax.ejb.Local;

/**
 * Interface indicates that a bean requires a login
 * procedure as a particular user.
 * 
 * @author hp
 *
 */
@Local
public interface LoginRequired {
	
	public void setLoggedInUserId(String personId);
	public String getLoggedInUserId();
}

