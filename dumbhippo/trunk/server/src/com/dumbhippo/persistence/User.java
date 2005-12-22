package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Person;

/**
 * The User is a person who is known to the system and thus
 * owns an account.
 * 
 * @author otaylor
 */
@Entity
@Table(name="HippoUser") // "User" is reserved in PostgreSQL
public class User extends Person implements VersionedEntity {
	private static final long serialVersionUID = 1L;
	
	private Account account;
	private int version;
	
	public User() {}
	
	// FIXME: delete this method; it is only used for the special 
	// theMan user, who is going away
	public User(Guid guid) {
		super(guid);
	}
	
	@OneToOne(fetch=FetchType.LAZY, mappedBy="owner")
	@JoinColumn(nullable=false)
	public Account getAccount() {
		return account;
	}
	
	void setAccount(Account account) {
		this.account = account;
	}
	
	@Column(nullable=false)
	public int getVersion() {
		return version;
	}
	
	public void setVersion(int version) {
		this.version = version;
	}
	
	@Override
	public String toString() {
		return "{User " + "guid = " + getId() + " nick = " + getNickname() + "}";
	}
}
