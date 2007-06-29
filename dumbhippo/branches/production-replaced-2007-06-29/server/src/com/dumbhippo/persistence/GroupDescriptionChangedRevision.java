package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class GroupDescriptionChangedRevision extends GroupRevision {

	private String newDescription;
	
	public GroupDescriptionChangedRevision(User revisor, Group target, Date time, String newDescription) {
		super(RevisionType.GROUP_DESCRIPTION_CHANGED, revisor, target, time);
		this.newDescription = newDescription;
	}

	protected GroupDescriptionChangedRevision() {
		
	}
	
	@Column(nullable=false)
	public String getNewDescription() {
		return newDescription;
	}

	protected void setNewDescription(String newDescription) {
		this.newDescription = newDescription;
	}
}
