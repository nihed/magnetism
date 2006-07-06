package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name="ExternalAccount", 
		   uniqueConstraints = 
			      {@UniqueConstraint(columnNames={"account_id", "accountType"})}
		   )
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class ExternalAccount extends DBUnique {
	private static final long serialVersionUID = 1L;

	private Account account;
	
	// what site is the account on
	private ExternalAccountType accountType;
	// how do we feel about this site
	private Sentiment sentiment;
	// meaning of this varies by the where
	private String handle;
	// meaning of this also varies by the where
	// not always used...
	private String extra;
	// quip (right now only applies if sentiment == HATE)
	private String quip;
	
	public ExternalAccount() {
		sentiment = Sentiment.INDIFFERENT;
	}
	
	public ExternalAccount(ExternalAccountType where) {
		this();
		this.accountType = where;
	}
	
	@ManyToOne
	@Column(nullable=false)
	public Account getAccount() {
		return account;
	}
	
	public void setAccount(Account account) {
		this.account = account;
	}
	
	@Column(nullable=false)
	public ExternalAccountType getAccountType() {
		return accountType;
	}
	public void setAccountType(ExternalAccountType type) {
		this.accountType = type;
	}
	
	@Column(nullable=true)
	public String getHandle() {
		return handle;
	}
	public void setHandle(String handle) {
		try {
			this.handle = accountType.canonicalizeHandle(handle);
		} catch (ValidationException e) {
			throw new IllegalArgumentException("Setting invalid handle on ExternalAccount type " + accountType + " value '" + handle + "'", e);
		}
	}

	public void setHandleValidating(String handle) throws ValidationException {
		this.handle = accountType.canonicalizeHandle(handle);
	}	
	
	@Column(nullable=true)
	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		try {
			this.extra = accountType.canonicalizeExtra(extra);
		} catch (ValidationException e) {
			throw new IllegalArgumentException("Setting invalid extra on ExternalAccount type " + accountType + " value '" + extra + "'", e);
		}
	}
	
	public void setExtraValidating(String extra) throws ValidationException {
		this.extra = accountType.canonicalizeExtra(extra);
	}
	
	@Column(nullable=true)
	public String getQuip() {
		return quip;
	}
	public void setQuip(String quip) {
		this.quip = quip;
	}
	
	@Column(nullable=false)
	public Sentiment getSentiment() {
		return sentiment;
	}
	public void setSentiment(Sentiment sentiment) {
		this.sentiment = sentiment;
	}
}
