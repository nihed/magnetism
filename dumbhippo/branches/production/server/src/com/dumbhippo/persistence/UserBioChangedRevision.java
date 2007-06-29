package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class UserBioChangedRevision extends UserRevision {

	private String newBio;	
	
	public UserBioChangedRevision(User revisor, Date time, String newBio) {
		super(RevisionType.USER_BIO_CHANGED, revisor, time);
		this.newBio = newBio;
	}

	protected UserBioChangedRevision() {
		
	}
	
	@Column(nullable=false)
	public String getNewBio() {
		return newBio;
	}

	protected void setNewBio(String newBio) {
		this.newBio = newBio;
	}
}
