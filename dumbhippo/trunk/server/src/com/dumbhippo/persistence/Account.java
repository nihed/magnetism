package com.dumbhippo.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;

import com.dumbhippo.Digest;
import com.dumbhippo.GlobalSetup;


/**
 * Object representing a Hippo account. Lots of things you might expect to be
 * here are not, because they're instead handled by saying that the Person in
 * question owns a Resource (see AccountClaim). So e.g. you your email address, home page, 
 * and other kinds of profile information.
 * If something can be in someone's contacts list without corresponding
 * to a registered Hippo user, then it can't be in the Account.
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
public class Account extends Resource {

	private static final Log logger = GlobalSetup.getLog(Account.class);	
	
	private static final long serialVersionUID = 0L;
		
	private User owner;
	
	private Set<Contact> contacts;
	
	private long creationDate;
	private int invitations;
	
	private boolean wasSentShareLinkTutorial;
	private boolean hasDoneShareLinkTutorial;
	
	private boolean disabled;
	
	private String password;
	
	/*
	 * don't add accessors to this directly, we don't want clients to "leak"
	 * very far since they have auth keys. Instead add methods that do whatever
	 * you need to do.
	 */
	private Set<Client> clients;
	
	/**
	 * Used only for Hibernate 
	 */
	protected Account() {
		this(null);
	}
	
	public Account(User owner) {	
		clients = new HashSet<Client>();
		contacts = new HashSet<Contact>();
		creationDate = -1;
		wasSentShareLinkTutorial = false;
		hasDoneShareLinkTutorial = false;
		disabled = false;
		
		if (owner != null) {
			this.owner = owner;
			if (owner.getAccount() == null) {
				owner.setAccount(this);
			} else {
				throw new RuntimeException("creating an account with User that already has one: " + owner);
			}
		}
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{Account " + getId() + " owner = ");
		if (owner != null)
			builder.append(owner.toString());
		else 
			builder.append("null");
		
		builder.append(" disabled = " + disabled);
		
		builder.append("}");
		// Don't dump clients/contacts here - otherwise we won't be
		// able to toString() a detached account
		
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
	@OneToMany(cascade = { CascadeType.ALL }, mappedBy="account")
	protected Set<Client> getClients() {
		return clients;
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
	@JoinColumn(nullable=false)
	public User getOwner() {
		return owner;
	}

	/**
	 * This is protected because calling it is probably 
	 * a bad idea. (Give someone else your account?)
	 * 
	 * @param owner The owner to set.
	 */
	protected void setOwner(User owner) {
		this.owner = owner;
	}

	@OneToMany(mappedBy="account")
	public Set<Contact> getContacts() {
		if (contacts == null)
			throw new RuntimeException("no contacts set???");
		return contacts;
	}

	/**
	 * This is protected because only Hibernate probably 
	 * needs to call it. Use addContact/removeContact 
	 * instead...
	 * 
	 * @param contacts your contacts
	 */
	protected void setContacts(Set<Contact> contacts) {
		if (contacts == null)
			throw new IllegalArgumentException("null contacts");
		this.contacts = contacts;
	}
	
	public void addContact(Contact contact) {
		if (contact == null)
			throw new IllegalArgumentException("null person");
		if (contacts == null)
			throw new RuntimeException("no contacts set???");
		contacts.add(contact);
	}
	
	public void addContacts(Set<Contact> contact) {
		if (contact == null)
			throw new IllegalArgumentException("null persons");
		contacts.addAll(contact);
	}
	
	public void removeContact(Contact contact) {
		contacts.remove(contact);
	}
	
	public void removeContacts(Set<Contact> contacts) {
		contacts.removeAll(contacts);
	}

	@Column(nullable=false)
	public Date getCreationDate() {
		if (creationDate < 0) {
			creationDate = System.currentTimeMillis();
		}
		return new Date(creationDate);
	}

	// only hibernate should call
	protected void setCreationDate(Date creationDate) {
		this.creationDate = creationDate.getTime();
	}

	@Column(nullable = false)
	public int getInvitations() {
		return invitations;
	}

	public void setInvitations(int invitations) {
		if (invitations < 0)
			throw new IllegalArgumentException("attempt to set negative invitation count: " + invitations);
		this.invitations = invitations;
	}
	
	public boolean canSendInvitations(int count) {
		return count <= invitations;
	}
	
	public void deductInvitations(int count) {
		if (count > invitations)
			throw new IllegalStateException("attempt to deduct more invitations than exist " + count + " from " + invitations);
		
		setInvitations(invitations - count);
	}
	
	@Column(nullable=false)
	public boolean getHasDoneShareLinkTutorial() {
		return hasDoneShareLinkTutorial;
	}

	public void setHasDoneShareLinkTutorial(boolean hasDoneShareLinkTutorial) {
		this.hasDoneShareLinkTutorial = hasDoneShareLinkTutorial;
	}

	@Column(nullable=false)
	public boolean getWasSentShareLinkTutorial() {
		return wasSentShareLinkTutorial;
	}

	public void setWasSentShareLinkTutorial(boolean wasSentShareLinkTutorial) {
		this.wasSentShareLinkTutorial = wasSentShareLinkTutorial;
	}

	/**
	 * This IS nullable.
	 * @return password if any or null
	 */
	protected String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		if (password != null)
			password = password.trim(); // paranoia
		this.password = password;
	}

	public boolean checkPassword(String attempt) {
		String correct = getPassword();
		if (correct == null)
			return false;
		return attempt.trim().equals(correct);
	}
	
	@Column(nullable=false)
	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	@Override
	@Transient
	public String getHumanReadableString() {
		return getOwner().getNickname();
	}
	
	@Override
	@Transient
	public String getDerivedNickname() {
		return getHumanReadableString();
	}
}
