package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.search.annotations.DocumentId;

@MappedSuperclass
public abstract class DBUnique implements Comparable {
	private long id;
	
	protected DBUnique() {
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
	
	// Used to sort a list by database ID
	public int compareTo(Object other) {
		if (!(other instanceof DBUnique))
			return -1; // why not
		
		long otherId = ((DBUnique)other).getId();
		if (this.id < otherId)
			return -1;
		else if (this.id > otherId)
			return 1;
		else
			return 0;
	}
}
