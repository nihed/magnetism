package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class GroupRevision extends Revision {

	private Group target;
	
	public GroupRevision(RevisionType type, User revisor, Group target, Date time) {
		super(type, revisor, time);
		this.target = target;
	}

	protected GroupRevision() {
		
	}
	
	@JoinColumn(nullable=false)
	@ManyToOne
	public Group getTarget() {
		return target;
	}

	protected void setTarget(Group target) {
		this.target = target;
	}
}
