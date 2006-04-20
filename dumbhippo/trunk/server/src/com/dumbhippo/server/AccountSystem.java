package com.dumbhippo.server;

import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;

@BanFromWebTier
@Local
public interface AccountSystem {
	
	/**
	 * Create a new Account owning the specified email
	 * address.  @see createAccountFromResource
	 * 
	 * @param email
	 * @return
	 */
	public Account createAccountFromEmail(String email);
	
	/**
	 * Adds a new Account (and Person) with verified ownership
	 * of the specified resource.  This relationship
	 * will be globally visible, and should have been (at least weakly) verified
	 * by some means (e.g. the person appears to have access to the specified
	 * resource)
	 * 
	 * @param email
	 * @return a new Person
	 */
	public Account createAccountFromResource(Resource res);
	
	/**
	 * Associate a new client with an account, with its own 
	 * authentication key.
	 * 
	 * @param acct account
	 * @param identifier a "name" for the client, @see Client
	 * @return a new client object
	 */
	public Client authorizeNewClient(Account acct, String name);
	
	/**
	 * Checks whether the user can authenticate with this auth key
	 * 
	 * @param user the user
	 * @param authKey their auth cookie
	 * @return true if authenticated
	 */
	public boolean checkClientCookie(User user, String authKey);
	
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
	public Set<Account> getActiveAccounts();
	
	/**
	 * Gets a list of accounts which have logged in in the last week.
	 * @return list of accounts
	 */
	public List<Account> getRecentlyActiveAccounts();
	
	/**
	 * Looks up an account by the User it's associated with.
	 * Throws a fatal runtime exception if no Account (should not happen)
	 * 
	 * This method is useless if the user is attached, 
	 * because you can just user.getAccount()
	 * 
	 * @param user the user
	 * @return their account
	 */
	public Account lookupAccountByUser(User user);
	
	/**
	 * Lookup an account by the GUID of the person who owns it.
	 * 
	 * @param personId person's ID
	 * @return account for this person Id, or null
	 */
	public Account lookupAccountByPersonId(String personId);

	/**
	 * Update the last login time for the account associated with the user.
	 * 
	 * @param userId the guid of a user
	 */
	public void touchLoginDate(Guid userId);		
}
