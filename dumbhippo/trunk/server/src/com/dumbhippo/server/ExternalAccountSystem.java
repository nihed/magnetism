package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.User;

@Local
public interface ExternalAccountSystem {
	/**
	 * Gets (creating if necessary) an ExternalAccount of the given type for the current user's viewpoint.
	 * 
	 * If created, the account starts out as Sentiment.INDIFFERENT; if already existing you have to 
	 * look at its current status.
	 * 
	 * @param viewpoint the logged-in user viewpoint
	 * @param type type of external account
	 * @return the account (never returns null)
	 */
	ExternalAccount getOrCreateExternalAccount(UserViewpoint viewpoint, ExternalAccountType type);
	
	/**
	 * Gets an external account for the given user, only if it already exists.
	 * Takes viewpoint into account (though currently the viewpoint doesn't matter).
	 *  
	 * Keep in mind that you then have to check whether the account is loved or hated.
	 *  
	 * @param viewpoint current user, anonymous, system viewpoint
	 * @param user user to get an account for
	 * @param type type of account
	 * @return the account
	 * @throws NotFoundException if the user in question has no account of this type
	 */
	ExternalAccount lookupExternalAccount(Viewpoint viewpoint, User user, ExternalAccountType type) throws NotFoundException;
	
	/**
	 * Gets all external accounts for a user, taking into account the current viewpoint.
	 * 
	 * Accounts may have any sentiment (loved, hated, indifferent), remember. 
	 *  
	 * @param viewpoint the current viewpoint
	 * @param user the user to get external accounts for
	 * @return the set of external accounts for this user (should not be modified)
	 */
	public Set<ExternalAccount> getExternalAccounts(Viewpoint viewpoint, User user);
}
