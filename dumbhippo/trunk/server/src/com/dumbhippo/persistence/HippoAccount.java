/**
 * 
 */
package com.dumbhippo.persistence;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.apache.commons.logging.Log;

import com.dumbhippo.Digest;
import com.dumbhippo.GlobalSetup;


/**
 * Object representing a Hippo account. Lots of things you might expect to be
 * here are not, because they're instead handled by saying that the Person in
 * question has a ResourceOwnershipClaim on a Resource. So e.g. you your email address, home page, 
 * and other kinds of profile information.
 * If something can be in someone's contacts list without corresponding
 * to a registered Hippo user, then it can't be in the HippoAccount.
 * 
 * Putting things in the hippo account is a little subjective, but roughly we
 * store things here if nobody but the account holder would have an opinion on
 * them. Fields in Person, on the other hand, may be shared among multiple 
 * account holders since the Person is a common object. Another way to
 * put it is that things in the Hippo account require registration with our
 * system, while you can have a Person in your contacts list with full name,
 * email, aim ID, etc. all without having that person registered.
 * 
 * @author hp
 * 
 */
@Entity
public class HippoAccount extends DBUnique implements Serializable {

	private static final Log logger = GlobalSetup.getLog(HippoAccount.class);	
	
	private static final long serialVersionUID = 0L;
		
	private Person owner;
	
	private Set<Person> contacts;
	
	/*
	 * don't add accessors to this directly, we don't want clients to "leak"
	 * very far since they have auth keys. Instead add methods that do whatever
	 * you need to do.
	 */
	private Set<Client> clients;
	
	private void initMissing() {
		if (clients == null)
			clients = new HashSet<Client>();
		if (contacts == null)
			contacts = new HashSet<Person>();
	}
	
	/**
	 * Used only for Hibernate 
	 */
	protected HippoAccount() {
		owner = null;
		initMissing();
	}
	
	public HippoAccount(Person person, Set<Client> clients) {
		owner = person;
		setClients(clients);
		initMissing();
	}
	
	public HippoAccount(Person person, Client initialClient) {		
		owner = person;
		Set<Client> c = new HashSet<Client>();
		c.add(initialClient);
		setClients(c);
		initMissing();
	}

	public HippoAccount(Person person) {	
		owner = person;
		initMissing();
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{Account " + getId() + " owner = ");
		if (owner != null)
			builder.append(owner.toString());
		else 
			builder.append("null");
		builder.append(" clients = { ");
		for (Client c : clients) {
			builder.append(c.toString());
			builder.append(" ");
		}
		builder.append("} }");
		
		return builder.toString();
	}
	
	/** 
	 * Adds a new client as authorized to login to the 
	 * account without any other password, etc. 
	 * Note, there is deliberately no API to 
	 * retrieve an already-existing client. We can create
	 * a new one only (though the new one is just as good for logging 
	 * in as the old ones)
	 *   
	 * @param client the new client to auth
	 */
	public void authorizeNewClient(Client client) {
		clients.add(client);
	}
	
	/**
	 * Checks whether a per-client authorization cookie exists and 
	 * has not expired.
	 * 
	 * @param authKey
	 * @return true if the cookie is OK for authorization
	 */
	public boolean checkClientCookie(String authKey) {
		logger.debug("comparing auth key");		
		for (Client client : clients) {
			String validKey = client.getAuthKey();
			logger.debug("comparing provided key \"" + authKey + "\" to valid key \"" + validKey + "\"");
			if (validKey.equals(authKey))
				return true;
		}
		return false;
	}
	
	/**
	 * Checks whether a per-client auth cookie exists and 
	 * has not expired, by prepending the given token 
	 * to the auth key, doing a SHA-1 hash, converting it 
	 * to an all-lowercase hex string, and seeing if it's 
	 * equal to the given digest.
	 * 
	 * @param token bytes to prepend to the digest
	 * @param digest the hex-encoded SHA-1 digest
	 * @return true if the cookie is OK for authorization
	 */
	public boolean checkClientCookie(String token, String digest) {
		Digest d = new Digest();
		for (Client client : clients) {
			String expectedDigest = d.computeDigest(token, client.getAuthKey());
			if (expectedDigest.equals(digest))
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
		return Collections.unmodifiableSet(clients);
	}

	protected void setClients(Set<Client> clients) {
		if (clients == null)
			throw new IllegalArgumentException("null");
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

	@ManyToMany
	public Set<Person> getContacts() {
		return Collections.unmodifiableSet(contacts);
	}

	/**
	 * This is protected because only Hibernate probably 
	 * needs to call it. Use addContact/removeContact 
	 * instead...
	 * 
	 * @param contacts your contacts
	 */
	protected void setContacts(Set<Person> contacts) {
		if (contacts == null)
			throw new IllegalArgumentException("null");
		this.contacts = contacts;
	}
	
	public void addContact(Person person) {
		contacts.add(person);
	}
	
	public void addContacts(Set<Person> persons) {
		contacts.addAll(persons);
	}
	
	public void removeContact(Person person) {
		contacts.remove(person);
	}
	
	public void removeContacts(Set<Person> persons) {
		contacts.removeAll(persons);
	}
}
