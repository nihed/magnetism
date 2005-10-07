/**
 * 
 */
package com.dumbhippo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;


/**
 * Object representing a Hippo account. Lots of things you might expect to be
 * here are not, because they're instead handled by saying that the Person in
 * question has a ResourceOwnershipClaim on a Resource. So e.g. you your email address, home page, 
 * and other kinds of profile information.
 * If something can be in someone's contacts/buddy list without corresponding
 * to a registered Hippo user, then it can't be in the HippoAccount.
 * 
 * Putting things in the hippo account is a little subjective, but roughly we
 * store things here if nobody but the account holder would have an opinion on
 * them (i.e. if the field is not relevant to the "buddy list"). Another way to
 * put it is that things in the Hippo account require registration with our
 * system, while you can have an item in your "buddy list" with full name,
 * email, aim ID, etc. all without having that person registered.
 * 
 * @author hp
 * 
 */
@Entity
public class HippoAccount extends DBUnique {

	private static final long serialVersionUID = 0L;
	
	private Person owner;
	private String username;
	/*
	 * don't add accessors to this directly, we don't want clients to "leak"
	 * very far since they have auth keys. Instead add methods that do whatever
	 * you need to do.
	 */
	private Set<Client> clients;
	
	/**
	 * @return Returns the username.
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Warning! Warning! The username can change over
	 * time (users can set it). The never-changing ID 
	 * for an account is the guid of the owner from 
	 * getOwner().
	 * 
	 * TODO the validation should be better (more permissive), and probably
	 * throw a checked exception. But just put something safe there for 
	 * now so we don't confuse Jabber; this has to be a valid JID.
	 * 
	 * @param username The username to set.
	 */
	public void setUsername(String username) {
		for (char c : username.toCharArray()) {
			if (!(Character.isLetter(c) || Character.isDigit(c))) {
				throw new IllegalArgumentException("Invalid username");
			}
		}
		this.username = username;
	}
	

	public HippoAccount(String username, Set<Client> clients) {
		setUsername(username);
		setClients(clients);
	}
	
	public HippoAccount(String username, Client initialClient) {
		setUsername(username);
		HashSet<Client> clients = new HashSet<Client>();
		clients.add(initialClient);		
		setClients(clients);
	}

	public String createClientCookie(String name) {
		Client client = new Client(name);
		clients.add(client);
		return client.getAuthKey();
	}
	
	/**
	 * Get the "primary" authentication key associated with this account.
	 * This method should be called only after a new HippoAccount has
	 * been created.  The results are undefined after multiple clients
	 * are associated.
	 * 
	 * @return an authentication key
	 */
	@Transient
	public String getInitialAuthKey() {
		return clients.iterator().next().getAuthKey();
	}

	/**
	 * Checks whether a given authorization key exists and 
	 * has not expired.
	 * 
	 * @param authKey
	 * @return true if the cookie is OK for authorization
	 */
	public boolean checkClientCookie(String authKey) {
		for (Client client : clients) {
			if (client.getAuthKey().equals(authKey))
				return true;
		}
		return false;
	}

	/**
	 * This is protected on purpose, so only the persistence 
	 * stuff can get at it and not you. Stay away.
	 * 
	 * @return the clients (programs/machines) used with this account
	 */
	@OneToMany
	protected Set<Client> getClients() {
		return clients;
	}

	protected void setClients(Set<Client> clients) {
		this.clients = clients;
	}

	/**
	 * The Person who owns this account. This is the 
	 * unique ID for the account.
	 * 
	 * @return Returns the owner.
	 */
	@OneToOne
	public Person getOwner() {
		return owner;
	}

	/**
	 * This is protected because calling it is probably 
	 * a bad idea. (Give someone else your account?)
	 * 
	 * @param owner The owner to set.
	 */
	protected void setOwner(Person owner) {
		this.owner = owner;
	}
}
