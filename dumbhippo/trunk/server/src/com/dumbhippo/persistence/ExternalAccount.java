package com.dumbhippo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class ExternalAccount extends DBUnique {
	private static final long serialVersionUID = 1L;

	private Account account;
	
	// what site is the account on
	private ExternalAccountType accountType;
	private OnlineAccountType onlineAccountType;
	// how do we feel about this site
	private Sentiment sentiment;
	// meaning of this varies by the where
	private String handle;
	// meaning of this also varies by the where
	// not always used...
	private String extra;
	// quip (right now only applies if sentiment == HATE)
	private String quip;
	// if the account has associated feeds, they go here
	private Set<Feed> feeds;
	// right now we should allow only up to one account of a given type to be Mugshot enabled
	private boolean mugshotEnabled;
	
	public ExternalAccount() {
		sentiment = Sentiment.INDIFFERENT;
		feeds = new HashSet<Feed>();
		mugshotEnabled = false;
	}
	
	public ExternalAccount(ExternalAccountType accountType) {
		this();
		this.accountType = accountType;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Account getAccount() {
		return account;
	}
	
	public void setAccount(Account account) {
		this.account = account;
	}
	
	// TODO: eventually, we should get rid of this column and use OnlineAccountType 
	@Column(nullable=true)
	public ExternalAccountType getAccountType() {
		return accountType;
	}
	public void setAccountType(ExternalAccountType type) {
		this.accountType = type;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public OnlineAccountType getOnlineAccountType() {
		return onlineAccountType;
	}
	
	public void setOnlineAccountType(OnlineAccountType onlineAccountType) {
		this.onlineAccountType = onlineAccountType;
	}
	
	@Column(nullable=true)
	public String getHandle() {
		return handle;
	}
	
	public void setHandle(String handle) {
		// This is called by Hibernate when loading objects from the database, so
		// we don't even want to do the minimal (now-web-service) validation work
		// of validateHandle()
		
		this.handle = handle;
	}

	public String validateHandle(String handle) throws ValidationException {
		if (accountType != null) {
			String validatedHandle = accountType.canonicalizeHandle(handle);
			if (accountType.requiresHandleIfLoved() && validatedHandle == null) {
				String usedForSite = "";
				if (!this.getAccountType().isInfoTypeProvidedBySite()) {
					usedForSite = " " + this.getSiteName() + " account";
				}
				
				throw new ValidationException("If you love " + this.getSiteName() + " let us know by entering your " 
						                      + this.getAccountType().getSiteUserInfoType() + usedForSite + ".");
			}		
			return validatedHandle;
		} else {
			if (handle.trim().length() == 0) {
				throw new ValidationException("If you use " + this.getSiteName() + " let us know by entering your " 
	                      + this.getOnlineAccountType().getUserInfoType() + ".");				
			}
			return handle;
		}
		
	}
	
	// used when setting a handle for loved accounts
	public void setHandleValidating(String handle) throws ValidationException {
		String validatedHandle = validateHandle(handle);
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
		if (accountType != null) {
		    String validatedExtra = accountType.canonicalizeExtra(extra);
		    if (accountType.requiresExtraIfLoved() && validatedExtra == null) {
			    // this message should not normally be displayed to the user
			    throw new ValidationException("Setting an empty value for " + this.getSiteName() + " user info extra is not valid");
		    }		
		    this.extra = validatedExtra;
		} else {
			this.extra = extra;
		}
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

    // This is a @ManyToMany relationship because:
    // -- an external account can have multiple feeds that we might want to poll
	// -- the same feed can be set on different external accounts (i.e. if two people
	//    have a shared photos account)
	@ManyToMany
	@JoinTable(name="ExternalAccount_Feed",
		       joinColumns=@JoinColumn(name="externalAccount_id", referencedColumnName="id"),                 
		       inverseJoinColumns=@JoinColumn(name="feed_id", referencedColumnName="id"),
		       uniqueConstraints=@UniqueConstraint(columnNames={"externalAccount_id", "feed_id"}))	
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	public Set<Feed> getFeeds() {
		return feeds;
	}

	public void setFeeds(Set<Feed> feeds) {
		if (feeds == null)
			throw new IllegalArgumentException("Setting feeds to null is illegal.");
		this.feeds = feeds;
	}
	
	public void addFeed(Feed feed) {
		feeds.add(feed);
	}
	
	public void removeFeed(Feed feed) {
		feeds.remove(feed);
	}
	
	@Transient	
	public Feed getFeed() {
		if (feeds.size() == 1)
			return feeds.toArray(new Feed[1])[0];
		else if (feeds.isEmpty())
			return null;
		else
			throw new RuntimeException("The external account has multiple feeds, not sure which feed you want me to return.");
	}
	
	@Column(nullable=false)
	public boolean isMugshotEnabled() {
		return mugshotEnabled;
	}

	public void setMugshotEnabled(boolean mugshotEnabled) {
		this.mugshotEnabled = mugshotEnabled;
	}

	public void setFeed(Feed feed) {
		Set<Feed> feedsSet = new HashSet<Feed>();
		feedsSet.add(feed);	
		setFeeds(feedsSet);
	}
	
	@Override
	public String toString() {
		return "{" + sentiment + " onlineAccountType=" + onlineAccountType + " handle=" + handle + " extra=" + extra + " quip=" + quip + "}";
	}
		
	@Transient
	public String getSiteName() {
		return onlineAccountType.getSiteName();
	}
	
	@Transient
	public String getLink() {
		if (hasAccountInfo())
			return accountType.getLink(handle, extra);
		else
			throw new RuntimeException("can't getLink() on account without handle/extra");
	}
	
	@Transient
	public String getSiteLink() {
	    return onlineAccountType.getSite();
	}
	
	@Transient
	public String getIconName() {
		if (accountType != null)
		    return accountType.getIconName();
		else
			return "";
	}
	
	@Transient
	public String getLinkText() {
		if (isLovedAndEnabled())
			return accountType.getLinkText(handle, extra);
		else
			throw new RuntimeException("can't getLinkText() on a not-loved-and-enabled account");
	}
	
	@Transient
	public boolean hasLovedAndEnabledType(ExternalAccountType type) {
		// checking for public page makes sure that we don't stack blocks for online.gnome.org users
		return accountType != null && accountType == type && getSentiment() == Sentiment.LOVE &&
		isMugshotEnabled() && getAccount().isActive() && getAccount().isPublicPage() && hasAccountInfo() &&
		(!accountType.isAffectedByMusicSharing() || getAccount().isMusicSharingEnabledWithDefault());
	}
	
	@Transient
	public String getAccountInfo() {
		if (accountType == null)
			return getHandle();
		
		switch (accountType.getInfoSource()) {
		case HANDLE:
			return getHandle();
		case EXTRA:
			return getExtra();
		case LINK:
			return getLink();
		case FEED:
			return getFeed().getSource().getUrl();		
		case CUSTOM:
			return "Your Account";
		default:
			throw new RuntimeException("Unexpected info source in ExternalAccount::getAccountInfo()");
		}
	}

        // This function is used for exporting the username via the data model. Over time we'll figure out what 
        // makes sense to return for accounts that have other info sources, such as Google Reader, Amazon, Rhapsody,
        // Netflix, and Facebook. The user info that we require for them right now is different from a username
        // or e-mail used by users for that service, and can make the most sense to the user in the form of a url,
        // which they can copy and paste into the browser to verify that it's their account.  
	@Transient
	public String getUsername() {
		return getAccountInfo();
	}
	
	@Transient
	public boolean isLovedAndEnabled() {
		return hasLovedAndEnabledType(accountType);
	}
	
	/** 
	 * I think we've declared this deprecated on the grounds that if sentiment is LOVE then 
	 * the account info is required to be there ... but there is probably code and/or old db state that doesn't 
	 * reflect this.
	 * @return
	 */
	@Transient
	public boolean hasAccountInfo() {
		return accountType != null && accountType.getHasAccountInfo(handle, extra);
	}
	
	public static int compare(ExternalAccount first, ExternalAccount second) {
		if (first.getOnlineAccountType().equals(second.getAccountType()))
			if (first.isMugshotEnabled())
				return -1;
			else if (second.isMugshotEnabled())
				return 1;
			else 
			    return 0;
		
		// We want "my website" first, "blog" second, then everything alphabetized by the human-readable name.
		
		if (first.getAccountType() == ExternalAccountType.WEBSITE)
			return -1;
		if (second.getAccountType() == ExternalAccountType.WEBSITE)
			return 1;
		if (first.getAccountType() == ExternalAccountType.BLOG)
			return -1;
		if (second.getAccountType() == ExternalAccountType.BLOG)
			return 1;				
		
		return String.CASE_INSENSITIVE_ORDER.compare(first.getOnlineAccountType().getFullName(), second.getOnlineAccountType().getFullName());
	}
}
