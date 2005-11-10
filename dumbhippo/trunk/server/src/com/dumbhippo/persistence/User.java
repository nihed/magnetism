package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Person;

/**
 * The User is a person who is known to the system and thus
 * owns an account.
 * 
 * @author otaylor
 */
@Entity
public class User extends Person {
	private static final long serialVersionUID = 1L;
	
	private Account account;
	
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
	
	protected void setAccount(Account account) {
		this.account = account;
	}
}
