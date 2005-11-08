package com.dumbhippo.persistence;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.EmbeddableSuperclass;
import javax.persistence.GeneratorType;
import javax.persistence.Id;

@EmbeddableSuperclass
public abstract class DBUnique implements Serializable {
	private long id;
	
	protected DBUnique() {
	}
	
	@Id(generate = GeneratorType.AUTO)
	@Column(nullable=false)
	public long getId() {
		return this.id;
	}
	
	protected void setId(long id) {
		this.id = id;
	}
}
