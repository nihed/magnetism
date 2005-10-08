/**
 * 
 */
package com.dumbhippo.persistence;

import java.io.Serializable;
import java.util.Collections;
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
public class HippoAccount extends DBUnique implements Serializable {

	private static final long serialVersionUID = 0L;
	
	private Person owner;
	/*
	 * don't add accessors to this directly, we don't want clients to "leak"
	 * very far since they have auth keys. Instead add methods that do whatever
	 * you need to do.
	 */
	private Set<Client> clients;
	
	
	/**
	 * Used only for Hibernate 
	 */
	protected HippoAccount() {
		owner = null;
		clients = null;
	}
	
	public HippoAccount(Person person, Set<Client> clients) {
		owner = person;
		setClients(clients);
	}
	
	public HippoAccount(Person person, Client initialClient) {
		owner = person;
		setClients(Collections.singleton(initialClient));
	}

	public HippoAccount(Person person) {
		owner = person;
		
		// the intermediate variable is needed
		// by sun javac but not ecj https://bugs.eclipse.org/bugs/show_bug.cgi?id=86898
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302214
		Set<Client> emptySet = Collections.emptySet();
		setClients(emptySet);
	}
	
	public HippoAccount(HippoAccount account) {
		//owner = account.owner;
		//setClients(account.clients);
		owner = new Person(account.owner);
		this.clients = new HashSet<Client>();
		for (Client c : account.clients) {
			this.clients.add(new Client(c));
		}
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("foo {Account owner = ");
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
		return Collections.unmodifiableSet(clients);
	}

	protected void setClients(Set<Client> clients) {
		this.clients = new HashSet<Client>(clients);
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
