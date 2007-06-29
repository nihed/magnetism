package com.dumbhippo.persistence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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
	
	private String stockPhoto;
	
	public User() {}
	
	// We use OneToMany here, because Hibernate can't cache
	// a OneToOne inverse relationship (!). We then wrap it
	// with a transient getter to get the singleton result.
	// This should be READ_ONLY, but it seems to be impossible
	// to properly initialize the combination of Acount
	// and User in that case.
	@OneToMany(fetch=FetchType.LAZY, mappedBy="owner")
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	protected Set<Account> getAccounts() {
		return accounts;
	}
	
	protected void setAccounts(Set<Account> accounts) {
		this.accounts = accounts;
	}
	
	@Transient
	public Account getAccount() {
		if (accounts.size() != 1) {
			throw new IllegalStateException("User should have an associated account, probably account never loaded before detaching, " + 
					accounts.size() + " accounts associated with: " + this);
		}
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
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	public Set<AccountClaim> getAccountClaims() {
		if (accountClaims == null)
			accountClaims = new HashSet<AccountClaim>();
		return accountClaims;
	}
	
	protected void setAccountClaims(Set<AccountClaim> accountClaims) {
		this.accountClaims = accountClaims;
	}	
	
	@Column(nullable=true)
	public String getStockPhoto() {
		return stockPhoto;
	}
	
	public void setStockPhoto(String stockPhoto) {
		// last-ditch check, we also want to check closer to where we got the 
		// photo from (e.g. on input from the wire)
		if (stockPhoto != null && !Validators.validateStockPhoto(stockPhoto))
			throw new RuntimeException("Set invalid stock photo on User");		
		this.stockPhoto = stockPhoto;
	}
	
	@Transient
	public String getPhotoUrl() {
		if (stockPhoto != null) {
			return "/images2" + stockPhoto;
		} else {
			return "/files/headshots/" + getId() + "?v=" + version;
		}
	}
	
	@Override
	public String toString() {
		return "{User " + "guid = " + getId() + " nick = " + getNickname() + "}";
	}
}
