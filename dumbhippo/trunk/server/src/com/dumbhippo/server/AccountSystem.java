package com.dumbhippo.server;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;

@Local
public interface AccountSystem {
	public static final boolean DEFAULT_ENABLE_MUSIC_SHARING = true;	
	public static final boolean DEFAULT_APPLICATION_USAGE_ENABLED = false;	
	
	/**
	 * Create a new Account owning the specified email
	 * address.  @see createAccountFromResource
	 * 
	 * @param email
	 * @return
	 * @throws ValidationException  if the email address is bogus
	 */
	public Account createAccountFromEmail(String email) throws ValidationException;
	
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
	 * Looks up a client for the user by the client ID
	 * 
	 * @param userId GUID of the user; this isn't actually used except for a sanity check.
	 * @param clientId ID of the client we are looking up
	 * @return the Client
	 */
	public Client getExistingClient(Guid userId, long clientId) throws NotFoundException;
	
	/**
	 * Checks whether the user with a specified ID can authenticate with this auth key,
	 * and returns the Account object for user. Throws an exception on failure.
	 * 
	 * @param guid the user's guid
	 * @param authKey their auth cookie
	 * @return the account object for the user
	 */
	public Account checkClientCookie(Guid guid, String authKey) throws NotFoundException, UnauthorizedException;
	
	public void updateWebActivity(User user);
	
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
	 * @param ownerId owner's ID
	 * @return account for this user Id; throws NotFoundException if the user doesn't exist
	 */
	public Account lookupAccountByOwnerId(Guid ownerId) throws NotFoundException;

	/**
	 * Gets one of our special users, like the music butterfly or 
	 * photo hippo or whatever. Supposed to be like any other user in 
	 * all respects, to avoid weird special cases. The only special case
	 * is that we autocreate the account.
	 *
	 * @return the character's User
	 */
	public User getCharacter(Character whichOne);

	/**
	 * Return the "preferences" for this user.  Currently just two keys:
	 *   musicSharingEnabled
	 *   musicSharingPrimed
	 * @param account user's account
	 * @return preferences mapping
	 */
	public Map<String, String> getPrefs(Account account);	
}
