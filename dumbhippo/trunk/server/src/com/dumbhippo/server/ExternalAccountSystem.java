package com.dumbhippo.server;

import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.Thumbnail;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.OnlineAccountType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.listeners.AccountStatusListener;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Local
public interface ExternalAccountSystem extends AccountStatusListener {
	/**
	 * Gets (creating if necessary) a Mugshot enabled ExternalAccount of the given type for the current user's viewpoint.
	 * 
	 * If created, the account starts out as Sentiment.INDIFFERENT; if already existing you have to 
	 * look at its current status.
	 * 
	 * @param viewpoint the logged-in user viewpoint
	 * @param type type of external account
	 * @return the account (never returns null)
	 */
	ExternalAccount getOrCreateExternalAccount(UserViewpoint viewpoint, ExternalAccountType type);
	
	// creates an ExternalAccount for the owner of the viepoint that is not Mugshot enabled
	public ExternalAccount createExternalAccount(UserViewpoint viewpoint, OnlineAccountType type);
	
	// looks up an ExternalAccount with the given id for the owner of the viewpoint
	public ExternalAccount lookupExternalAccount(UserViewpoint viewpoint, String id) throws NotFoundException;
	
	/**
	 * Gets an external account for the given user, only if it already exists and is Mugshot-enabled.
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
	 * Gets existing external accounts for the given user  of a specified type.
	 * Takes viewpoint into account (though currently the viewpoint doesn't matter).
	 *  
	 * Keep in mind that you then have to check whether the accounts are loved or hated.
	 *  
	 * @param viewpoint current user, anonymous, system viewpoint
	 * @param user user to get an account for
	 * @param type online account type of accounts
	 * @return the accounts
	 */
	public Set<ExternalAccount> lookupExternalAccounts(Viewpoint viewpoint, User user, OnlineAccountType type);
	
	/**
	 * Checks whether the given external account exists, is loved, and the user's account is enabled. i.e. 
	 * returns TRUE if we should be using this account. Equivalent to calling ExternalAccount.isLovedAndEnabled()
	 * after looking up the account by type.
	 * @param viewpoint
	 * @param user
	 * @param accountType
	 * @return
	 */
	public boolean getExternalAccountExistsLovedAndEnabled(Viewpoint viewpoint, User user, ExternalAccountType accountType);
	
	/**
	 * Gets all external account views for a user, taking into account the current viewpoint.
	 * 
	 * Accounts may have any sentiment (loved, hated, indifferent), remember. 
	 *  
	 * @param viewpoint the current viewpoint
	 * @param user the user to get external accounts for
	 * @return the set of external account views for this user (should not be modified)
	 */
	public Set<ExternalAccountView> getExternalAccountViews(Viewpoint viewpoint, User user);
	
	public ExternalAccountView getExternalAccountView(Viewpoint viewpoint, ExternalAccount externalAccount);
	
	public ExternalAccountView getExternalAccountView(Viewpoint viewpoint, User user, ExternalAccountType externalAccountType) throws NotFoundException;
	
	public void loadThumbnails(Viewpoint viewpoint, Set<ExternalAccountView> accountViews);
	
	// NOTE: returns null if external account doesn't have thumbnails
	public List<? extends Thumbnail> getThumbnails(ExternalAccount externalAccount);

	public void setSentiment(ExternalAccount externalAccount, Sentiment sentiment);
	
	public void validateAll();
	
	public OnlineAccountType getOnlineAccountType(ExternalAccountType accountType);
	
	public List<OnlineAccountType> getAllOnlineAccountTypes();

	public void writeSupportedOnlineAccountTypesToXml(XmlBuilder xml, String lang); 
	
	public OnlineAccountType lookupOnlineAccountTypeForName(String name) throws NotFoundException;
	
	public OnlineAccountType lookupOnlineAccountTypeForFullName(String fullName) throws NotFoundException;
	
	public OnlineAccountType lookupOnlineAccountTypeForUserInfoType(String userInfoType) throws NotFoundException;

	public List<OnlineAccountType> lookupOnlineAccountTypesForSite(String siteUrl);
	
	public OnlineAccountType createOnlineAccountType(UserViewpoint viewpoint, String name, String fullName, String siteName, String siteUrl, String userInfoType) throws ValidationException;	
}
