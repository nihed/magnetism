package com.dumbhippo.server;

import javax.ejb.Stateless;

import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Resource;

@Stateless
public interface AccountSystem {
	/**
	 * Adds a new HippoAccount (and Person) with verified ownership
	 * of the specified resource.  This relationship
	 * will be globally visible, and should have been (at least weakly) verified
	 * by some means (e.g. the person appears to have access to the specified
	 * resource)
	 * 
	 * @param email
	 * @return a new Person
	 */
	public HippoAccount createAccountFromResource(Resource res);
}
