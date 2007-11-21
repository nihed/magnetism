package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class FacebookResource extends Resource {
	
	private static final long serialVersionUID = 0L;
	
	private String facebookUserId;
	
	protected FacebookResource() {}
	
	public FacebookResource(String facebookUserId) {
		this.facebookUserId = facebookUserId;
	}

	@Column(unique=true, nullable=false)
	public String getFacebookUserId() {
		return facebookUserId;
	}
	
	protected void setFacebookUserId(String facebookUserId) {
		this.facebookUserId = facebookUserId;
	}
	
	@Override
	@Transient
	public String getHumanReadableString() {
		return "Facebook user " + getFacebookUserId();
	}
		
	@Override
	@Transient
	public String getDerivedNickname() {
		return getHumanReadableString();
	}
}
