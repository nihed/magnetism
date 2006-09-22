package com.dumbhippo.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

@Entity
public class FacebookAccount extends DBUnique {
	
	private static final long serialVersionUID = 1L;

	private String facebookApiUserId;
	private String sessionKey;
	private boolean sessionKeyValid;
	private ExternalAccount externalAccount;
	private int unreadMessageCount;
	private int totalMessageCount;
	private long messageCountTimestamp;
	private int wallMessageCount;
	private int unseenPokeCount;
	private int totalPokeCount;
	private long pokeCountTimestamp;
	private Set<FacebookEvent> facebookEvents; 
	// photos tagged with the user
	private Set<FacebookPhotoData> taggedPhotos;
	private boolean taggedPhotosPrimed;
	
	protected FacebookAccount() {}
	
	public FacebookAccount(ExternalAccount externalAccount) {
		assert(externalAccount.getAccountType().equals(ExternalAccountType.FACEBOOK));
		this.externalAccount = externalAccount;
		this.sessionKeyValid = false;
		this.unreadMessageCount = -1;
		this.totalMessageCount = -1;
		this.messageCountTimestamp = -1;
		this.wallMessageCount = -1;
		this.unseenPokeCount = -1;
		this.totalPokeCount = -1;
		this.pokeCountTimestamp = -1;
		this.facebookEvents = new HashSet<FacebookEvent>();
		this.taggedPhotos = new HashSet<FacebookPhotoData>();	
		this.taggedPhotosPrimed = false;
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

	@Column(nullable=false)
	public Date getMessageCountTimestamp() {
		return new Date(messageCountTimestamp);
	}

	@Transient
	public long getMessageCountTimestampAsLong() {
		return messageCountTimestamp;
	}
	
	public void setMessageCountTimestamp(Date messageCountTimestamp) {
		this.messageCountTimestamp = messageCountTimestamp.getTime();
	}
	
	public void setMessageCountTimestampAsLong(long messageCountTimestamp) {
		this.messageCountTimestamp = messageCountTimestamp;
	}
	
    @OneToMany(mappedBy="facebookAccount")
	public Set<FacebookEvent> getFacebookEvents() {
		return facebookEvents;
	}

	public void setFacebookEvents(Set<FacebookEvent> facebookEvents) {
		this.facebookEvents = facebookEvents;
	}

	public void addFacebookEvent(FacebookEvent facebookEvent) {
		facebookEvents.add(facebookEvent);
	}
	
	@Column(nullable=false)
	public int getWallMessageCount() {
		return wallMessageCount;
	}

	public void setWallMessageCount(int wallMessageCount) {
		this.wallMessageCount = wallMessageCount;
	}
	
	@Column(nullable=false)
	public int getUnseenPokeCount() {
		return unseenPokeCount;
	}
	
    public void setUnseenPokeCount(int unseenPokeCount) {
    	this.unseenPokeCount = unseenPokeCount;
    }

	@Column(nullable=false)
	public int getTotalPokeCount() {
		return totalPokeCount;
	}
	
    public void setTotalPokeCount(int totalPokeCount) {
    	this.totalPokeCount = totalPokeCount;
    }

	@Column(nullable=false)
	public Date getPokeCountTimestamp() {
		return new Date(pokeCountTimestamp);
	}

	@Transient
	public long getPokeCountTimestampAsLong() {
		return pokeCountTimestamp;
	}
	
	public void setPokeCountTimestamp(Date pokeCountTimestamp) {
		this.pokeCountTimestamp = pokeCountTimestamp.getTime();
	}
	
	public void setPokeCountTimestampAsLong(long pokeCountTimestamp) {
		this.pokeCountTimestamp = pokeCountTimestamp;
	}
	
    // This is one to many because we don't know if facebook will return 
	// the same photo data when requests are made for different facebook
	// users who are in the same photo, so we just store all photos returned
	// for each account.
	@OneToMany(mappedBy="facebookAccount")
	public Set<FacebookPhotoData> getTaggedPhotos() {
		return taggedPhotos;
	}
	
	public void setTaggedPhotos(Set<FacebookPhotoData> taggedPhotos) {
		if (taggedPhotos == null)
			throw new IllegalArgumentException("null taggedPhotos");
		this.taggedPhotos = taggedPhotos;
	}
	
	public void addTaggedPhoto(FacebookPhotoData taggedPhoto) {
		taggedPhotos.add(taggedPhoto);
	}
	
	public void removeTaggedPhoto(FacebookPhotoData taggedPhoto) {
		taggedPhotos.remove(taggedPhoto);
	}
	
	@Column(nullable=false)
	public boolean getTaggedPhotosPrimed() {
		return taggedPhotosPrimed;
	}

	public void setTaggedPhotosPrimed(boolean taggedPhotosPrimed) {
		this.taggedPhotosPrimed = taggedPhotosPrimed;
	}
}
