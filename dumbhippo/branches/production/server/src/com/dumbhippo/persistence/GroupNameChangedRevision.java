package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class GroupNameChangedRevision extends GroupRevision {

	private String newName;
	
	public GroupNameChangedRevision(User revisor, Group target, Date time, String newName) {
		super(RevisionType.GROUP_NAME_CHANGED, revisor, target, time);
		this.newName = newName;
	}

	protected GroupNameChangedRevision() {
		
	}
	
	@Column(nullable=false)
	public String getNewName() {
		return newName;
	}

	protected void setNewName(String newName) {
		this.newName = newName;
	}
}
