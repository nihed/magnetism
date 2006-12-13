/**
 * 
 */
package com.dumbhippo.persistence;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.dumbhippo.identity20.RandomToken;

/**
 * 
 * A client is a computer or other device or application that someone uses to
 * access their account. The main purpose of this class is to store an auth cookie 
 * for the client so the user doesn't have to log in again, but it 
 * could also be used for random per-client state.
 * 
 * @author hp
 * 
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class Client extends DBUnique implements Serializable {

	private static final long serialVersionUID = 0L;

	private Account account;

	private String authKey;

	private String name;
	
	// store date in this form since it's immutable and lightweight
	private long lastUsed;

	private Client(Account account, String authKey, String name, long lastUsed) {
		this.account = account;
		this.authKey = authKey;
		if (this.authKey == null)
			this.authKey = RandomToken.createNew().toString();
		this.name = name;
		this.lastUsed = lastUsed;
	}

	public Client(Account account) {
		this(account, null, "Anonymous", // TODO localize
				System.currentTimeMillis());
		
	}
	
	protected Client() {}

	public Client(Account account, String name) {
		this(account, null, name, System.currentTimeMillis());
	}
	
	public Client(Client client) {
		this(client.account, client.authKey, client.name, client.lastUsed);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{Client name = " + name + " authKey " + "???" + " lastUsed " + lastUsed + "}");
		return builder.toString();
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Account getAccount() {
		return account;
	}
	
	public void setAccount(Account account) {
		this.account = account;
	}
	
	@Column(nullable=false,length=RandomToken.STRING_LENGTH)
	public String getAuthKey() {
		return authKey;
	}

	public void setAuthKey(String authKey) {
		this.authKey = authKey;
	}

	@Column(nullable=false)
	public Date getLastUsed() {
		return new Date(lastUsed);
	}

	public void setLastUsed(Date lastUsed) {
		this.lastUsed = lastUsed.getTime();
	}

	/**
	 * A string that might give some hint about the sort of client.
	 * Might be the IP address of the client, 
	 * or the name of the chat app, or "home" or whatever.
	 * 
	 * Probably users can set this, similar to a jabber resource?
	 * 
	 * @return the name
	 */
	@Column(nullable=false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
