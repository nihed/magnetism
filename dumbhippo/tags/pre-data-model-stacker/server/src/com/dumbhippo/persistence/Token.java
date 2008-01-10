package com.dumbhippo.persistence;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import com.dumbhippo.identity20.RandomToken;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Token extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private String authKey;
	
	// store date in this form since it's immutable and lightweight
	private long creationDate;
	
	private boolean deleted;
	
	// constructor for hibernate to use
	protected Token() {
	}
	
	// constructor for subclasses to use, they'll always use initialize=true
	protected Token(boolean initialize) {
		if (initialize) {
			this.authKey = RandomToken.createNew().toString();
			this.creationDate = System.currentTimeMillis();
			this.deleted = false;
		}
	}
	
	@Column(nullable=false,unique=true,length=RandomToken.STRING_LENGTH)
	public String getAuthKey() {
		return authKey;
	}
	protected void setAuthKey(String authKey) {
		this.authKey = authKey;
	}
	
	@Column(nullable=false)
	public Date getCreationDate() {
		return new Date(creationDate);
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate.getTime();
	}

	/**
	 * For now only used for InvitationToken, but probably should be cleaned up to 
	 * be used for all of them. Since it's only used by InvitationToken it's checked in
	 * VerifyServlet instead of TokenSystemBean.
	 * 
	 * @return whether the token is deleted
	 */
	@Column(nullable=false)
	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	@Transient
	public String getPartialAuthURL() {
		return "verify?authKey=" + getAuthKey();
	}
	
	@Transient
	public String getAuthURL(URL prefix) {
		URL authURL;
		try {
			authURL = new URL(prefix, getPartialAuthURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return authURL.toString();
	}

	@Transient
	public String getAuthURL(String prefix) {
		try {
			return getAuthURL(new URL(prefix));
		} catch (MalformedURLException e) {
			throw new RuntimeException("Bad url", e);
		}
	}
	
	@Transient
	protected long getExpirationPeriodInSeconds() {
		return 60*60; // 1 hour
	}
	
	@Transient
	public boolean isExpired() {
		long age = (System.currentTimeMillis() - getCreationDate().getTime()) / 1000;
		return (age < 0 || age > getExpirationPeriodInSeconds());	
	}
	
	/**
	 * For now this only applies to InvitationToken, since isDeleted only does
	 * @return whether the token should be honored
	 */
	@Transient
	public boolean isValid() {
	    return (!isDeleted() && !isExpired());	
	}
	
	@Override
	public String toString() {
		return getAuthKey() + "@[" + getCreationDate() + "]";
	}
}
