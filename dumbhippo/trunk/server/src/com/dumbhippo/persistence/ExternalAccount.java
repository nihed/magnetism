package com.dumbhippo.persistence;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
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
 
	private List<String> thumbnailUrls;
	private int thumbnailTotalItems;
	private int thumbnailWidth;
	private int thumbnailHeight;
	
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

	public void setHandleValidating(String handle) throws ValidationException {
		this.handle = accountType.canonicalizeHandle(handle);
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
	public String getLinkText() {
		return accountType.getLinkText(handle, extra);
	}

	/**
	 * Sets thumbnails for this account; done in the "view" tier. This thumbnail stuff
	 * would typically be on a "view" object like PersonView, TrackView but in this
	 * case it doesn't seem worth creating and managing an ExternalAccountView. We'll see 
	 * how it goes.
	 * 
	 * @param thumbnailUrls
	 * @param width
	 * @param height
	 */
	public void setThumbnails(List<String> thumbnailUrls, int totalItems, int width, int height) {
		this.thumbnailUrls = thumbnailUrls;
		this.thumbnailWidth = width;
		this.thumbnailHeight = height;
		this.thumbnailTotalItems = totalItems;
	}
	
	@Transient
	public boolean getHasThumbnails() {
		return thumbnailUrls != null;
	}
	
	@Transient
	public List<String> getThumbnailUrls() {
		return thumbnailUrls;
	}
	
	@Transient
	public int getThumbnailCount() {
		if (thumbnailUrls != null)
			return thumbnailUrls.size();
		else
			return 0;
	}
	
	@Transient
	public int getThumbnailWidth() {
		return thumbnailWidth;
	}
	
	@Transient
	public int getThumbnailHeight() {
		return thumbnailHeight;
	}
	
	@Transient
	public int getTotalThumbnailItems() {
		return thumbnailTotalItems; 
	}
	
	@Transient
	public String getTotalThumbnailItemsString() {
		return accountType.formatThumbnailCount(thumbnailTotalItems); 
	}
}
