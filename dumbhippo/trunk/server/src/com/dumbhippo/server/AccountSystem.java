package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Person;
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
	public Client authorizeNewClient(HippoAccount acct, String name);
	
	/** 
	 * Gets the number of active accounts.
	 * 
	 * @return number of active accounts
	 */
	public long getNumberOfActiveAccounts();
	
	/** 
	 * Gets a list of all active accounts. NOT EFFICIENT. Test suite 
	 * usage only...
	 * @return all active accounts in the system
	 */
	public Set<HippoAccount> getActiveAccounts();
	
	/**
	 * Looks up an account by the Person it's associated with. 
	 * If this function returns non-null, then a Person is 
	 * registered with our system. If it returns null, then 
	 * a person is an implicit person we think is out there,
	 * but hasn't signed up.
	 * 
	 * @param person the person
	 * @return their account or null if they don't have one
	 */
	public HippoAccount lookupAccountByPerson(Person person);
	
	/**
	 * Lookup an account by the GUID of the person who owns it.
	 * 
	 * @param personId person's ID
	 * @return account for this person Id, or null
	 */
	public HippoAccount lookupAccountByPersonId(String personId);		
}
