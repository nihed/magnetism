package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Resource;

@Local
public interface AccountSystem {
	
	/**
	 * Create a new HippoAccount owning the specified email
	 * address.  @see createAccountFromResource
	 * 
	 * @param email
	 * @return
	 */
	public HippoAccount createAccountFromEmail(String email);
	
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
	
	/**
	 * Associate a new client with an account, with its own 
	 * authentication key.
	 * 
	 * @param acct account
	 * @param identifier a "name" for the client, @see Client
	 * @return a new client object
	 */
	public Client addClient(HippoAccount acct, String identifier);
}
