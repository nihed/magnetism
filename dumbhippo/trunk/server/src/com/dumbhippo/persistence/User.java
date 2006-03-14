package com.dumbhippo.persistence;

import java.util.Collections;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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
	
	private Set<Account> accounts;
	private Set<AccountClaim> accountClaims;

	private int version;
	
	public User() {}
	
	// FIXME: delete this method; it is only used for the special 
	// theMan user, who is going away
	public User(Guid guid) {
		super(guid);
	}
	
	// We use OneToMany here, because Hibernate can't cache
	// a OneToOne inverse relationship (!). We then wrap it
	// with a transient getter to get the singleton result.
	@OneToMany(fetch=FetchType.LAZY, mappedBy="owner")
	@Cache(usage=CacheConcurrencyStrategy.READ_ONLY)
	protected Set<Account> getAccounts() {
		return accounts;
	}
	
	protected void setAccounts(Set<Account> accounts) {
		this.accounts = accounts;
	}
	
	@Transient
	public Account getAccount() {
		return accounts.iterator().next();
	}
	
	@Transient
	public void setAccount(Account account) {
		this.accounts = Collections.singleton(account);
	}
	
	@Column(nullable=false)
	public int getVersion() {
		return version;
	}
	
	public void setVersion(int version) {
		this.version = version;
	}
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="owner")
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	public Set<AccountClaim> getAccountClaims() {
		return accountClaims;
	}
	
	protected void setAccountClaims(Set<AccountClaim> accountClaims) {
		this.accountClaims = accountClaims;
	}	
	
	@Override
	public String toString() {
		return "{User " + "guid = " + getId() + " nick = " + getNickname() + "}";
	}
}
