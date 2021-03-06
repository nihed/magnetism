package com.dumbhippo.persistence;

import javax.persistence.EmbeddableSuperclass;
import javax.persistence.GeneratorType;
import javax.persistence.Id;

@EmbeddableSuperclass
public abstract class DBUnique {
	protected long id;
	
	protected DBUnique() {
	}
	
	@Id(generate = GeneratorType.AUTO)
	public long getId() {
		return this.id;
	}
	
	protected void setId(long id) {
		this.id = id;
	}
}
