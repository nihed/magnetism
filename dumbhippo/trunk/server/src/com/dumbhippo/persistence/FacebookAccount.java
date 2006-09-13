package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Entity
public class FacebookAccount extends DBUnique {
	
	private static final long serialVersionUID = 1L;

	private String facebookApiUserId;
	private String sessionKey;
	private boolean sessionKeyValid;
	private ExternalAccount externalAccount;
	private int unreadMessageCount;
	private int totalMessageCount;
	
	private FacebookAccount() {
	}
	
	public FacebookAccount(ExternalAccount externalAccount) {
		assert(externalAccount.getAccountType().equals(ExternalAccountType.FACEBOOK));
		this.externalAccount = externalAccount;
		this.sessionKeyValid = false;
		this.unreadMessageCount = -1;
		this.totalMessageCount = -1;
	}
	
	@Column(nullable=true)
	public String getFacebookUserId() {
		return facebookApiUserId;
	}
	
	public void setFacebookUserId(String facebookApiUserId) {
		this.facebookApiUserId = facebookApiUserId;
	}
	
	@Column(nullable=true)
	public String getSessionKey() {
		return sessionKey;
	}
	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}	
	
	@Column(nullable=false)
	public boolean isSessionKeyValid() {
		return sessionKeyValid;
	}

	public void setSessionKeyValid(boolean sessionKeyValid) {
		this.sessionKeyValid = sessionKeyValid;
	}
	
	@OneToOne
	@JoinColumn(nullable=false)
	public ExternalAccount getExternalAccount() {
		return externalAccount;
	}
	
	public void setExternalAccount(ExternalAccount externalAccount) {
		this.externalAccount = externalAccount;
	}
	
	@Column(nullable=false)
	public int getUnreadMessageCount() {
		return unreadMessageCount;
	}
	
    public void setUnreadMessageCount(int unreadMessageCount) {
    	this.unreadMessageCount = unreadMessageCount;
    }

	@Column(nullable=false)
	public int getTotalMessageCount() {
		return totalMessageCount;
	}
	
    public void setTotalMessageCount(int totalMessageCount) {
    	this.totalMessageCount = totalMessageCount;
    }
}
