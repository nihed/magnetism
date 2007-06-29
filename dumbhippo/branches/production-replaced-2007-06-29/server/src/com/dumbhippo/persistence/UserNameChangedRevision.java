package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class UserNameChangedRevision extends UserRevision {

	private String newName;	
	
	public UserNameChangedRevision(User revisor, Date time, String newName) {
		super(RevisionType.USER_NAME_CHANGED, revisor, time);
		this.newName = newName;
	}

	protected UserNameChangedRevision() {
		
	}
	
	@Column(nullable=false)
	public String getNewName() {
		return newName;
	}

	protected void setNewName(String newName) {
		this.newName = newName;
	}
	
	
}
