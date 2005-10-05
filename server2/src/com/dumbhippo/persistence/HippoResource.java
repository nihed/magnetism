/**
 * 
 */
package com.dumbhippo.persistence;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Transient;


/**
 * Resource representing a Hippo account. Lots of things you might expect to be
 * here are not, because they're instead handled by saying that the Person in
 * question has a ResourceOwnershipClaim on a Resource. So e.g. you own your
 * full name, your email address, and other kinds of profile information.
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
public class HippoResource extends Resource {

	/*
	 * don't add accessors to this directly, we don't want clients to "leak"
	 * very far since they have auth keys. Instead add methods that do whatever
	 * you need to do.
	 */
	private Set<Client> clients;

	public HippoResource() {
		
	}
	
	@Override
	@Transient
	public String getHumanReadableString() {
		return null;
	}	
	
	public String createClientCookie(String name) {
		Client client = new Client(name);
		clients.add(client);
		return client.getAuthKey();
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

	@OneToMany
	protected Set<Client> getClients() {
		return clients;
	}

	protected void setClients(Set<Client> clients) {
		this.clients = clients;
	}
}
