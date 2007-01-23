package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name="ExternalAccount", 
		   uniqueConstraints = 
			      {@UniqueConstraint(columnNames={"account_id", "accountType"})}
		   )
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
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
	// if the account has an associated feed, that goes here
	private Feed feed;
	
	public ExternalAccount() {
		sentiment = Sentiment.INDIFFERENT;
	}
	
	public ExternalAccount(ExternalAccountType where) {
		this();
		this.accountType = where;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
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
		if (accountType == null) {
			// this happens if hibernate sets this field before the accountType field
			// in that case we just skip validation... in theory stuff in the db is valid.
			this.handle = handle;
			return;
		}
		
		try {
			this.handle = accountType.canonicalizeHandle(handle);
		} catch (ValidationException e) {
			throw new IllegalArgumentException("Setting invalid handle on ExternalAccount type " + accountType + " value '" + handle + "'", e);
		}
	}

	// used when setting a handle for loved accounts
	public void setHandleValidating(String handle) throws ValidationException {
		String validatedHandle = accountType.canonicalizeHandle(handle);
		if (accountType.requiresHandleIfLoved() && validatedHandle == null) {
			String usedForSite = "";
			if (!this.getAccountType().isInfoTypeProvidedBySite()) {
				usedForSite = " " + this.getSiteName() + " account";
			}
			
			throw new ValidationException("If you love " + this.getSiteName() + " let us know by entering your " 
					                      + this.getAccountType().getSiteUserInfoType() + usedForSite);
		}
		this.handle = validatedHandle;
	}	
	
	@Column(nullable=true)
	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		if (accountType == null) {
			// this happens if hibernate sets this field before the accountType field
			// in that case we just skip validation... in theory stuff in the db is valid.
			this.extra = extra;
			return;
		}
		
		try {
			this.extra = accountType.canonicalizeExtra(extra);
		} catch (ValidationException e) {
			throw new IllegalArgumentException("Setting invalid extra on ExternalAccount type " + accountType + " value '" + extra + "'", e);
		}
	}

	// used when setting an extra for loved accounts
	public void setExtraValidating(String extra) throws ValidationException {
		String validatedExtra = accountType.canonicalizeExtra(extra);
		if (accountType.requiresExtraIfLoved() && validatedExtra == null) {
			// this message should not normally be displayed to the user
			throw new ValidationException("Setting an empty value for " + this.getSiteName() + " user info extra is not valid");
		}
		this.extra = validatedExtra;
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
	/** Don't call directly, use ExternalAccountSystem.setSentiment */
	public void setSentiment(Sentiment sentiment) {
		this.sentiment = sentiment;
	}

	@ManyToOne
	@JoinColumn(nullable=true)	
	public Feed getFeed() {
		return feed;
	}

	public void setFeed(Feed feed) {
		this.feed = feed;
	}
	
	@Override
	public String toString() {
		return "{" + sentiment + " accountType=" + accountType + " handle=" + handle + " extra=" + extra + " quip=" + quip + "}";
	}
		
	@Transient
	public String getSiteName() {
		return accountType.getSiteName();
	}
	
	@Transient
	public String getLink() {
		return accountType.getLink(handle, extra);
	}
	
	@Transient
	public String getSiteLink() {
	    if (!accountType.getSiteLink().equals("")) {
	    	return accountType.getSiteLink();
	    } else {
	    	return getLink();
	    }
	}
	
	@Transient
	public String getIconName() {
		return accountType.getIconName();
	}
	
	@Transient
	public String getLinkText() {
		return accountType.getLinkText(handle, extra);
	}
	
	@Transient
	public boolean hasLovedAndEnabledType(ExternalAccountType type) {
		return accountType == type && getSentiment() == Sentiment.LOVE &&
		getAccount().isActive();
	}
	
	@Transient
	public boolean isLovedAndEnabled() {
		return hasLovedAndEnabledType(accountType);
	}
	
	@Transient
	public boolean hasAccountInfo() {
		return accountType.getHasAccountInfo(handle, extra);
	}
}
