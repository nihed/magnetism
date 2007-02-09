package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class UserRevision extends Revision {

	protected UserRevision(RevisionType type, User revisor, Date time) {
		super(type, revisor, time);
	}

	protected UserRevision() {
		
	}
	
}
