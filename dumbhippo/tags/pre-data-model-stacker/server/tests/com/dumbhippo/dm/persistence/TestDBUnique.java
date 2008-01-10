package com.dumbhippo.dm.persistence;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.search.annotations.DocumentId;

@MappedSuperclass
public class TestDBUnique {
	private long id;
	
	protected TestDBUnique() {
	}
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(nullable=false)
	@DocumentId // No effect except for subclasses that are @Indexed
	public long getId() {
		return this.id;
	}
	
	protected void setId(long id) {
		this.id = id;
	}
}
