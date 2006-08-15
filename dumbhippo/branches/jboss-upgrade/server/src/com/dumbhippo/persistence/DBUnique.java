package com.dumbhippo.persistence;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.lucene.Keyword;

@MappedSuperclass
public abstract class DBUnique implements Serializable {
	private long id;
	
	protected DBUnique() {
	}
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(nullable=false)
	@Keyword(id=true) // No effect except for subclasses that are @Indexed
	public long getId() {
		return this.id;
	}
	
	protected void setId(long id) {
		this.id = id;
	}
}
