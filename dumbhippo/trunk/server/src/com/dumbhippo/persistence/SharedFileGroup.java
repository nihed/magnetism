package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name="SharedFileGroup", 
		   uniqueConstraints = 
			      {@UniqueConstraint(columnNames={"file_id", "group_id"})}
		   )
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class SharedFileGroup extends DBUnique {

	private static final long serialVersionUID = 1L;

	private SharedFile file;
	private Group group;
	
	protected SharedFileGroup() {
	}
	
	public SharedFileGroup(SharedFile file, Group group) {
		this.file = file;
		this.group = group;
	}

	@ManyToOne
	@JoinColumn(nullable=false)
	public SharedFile getFile() {
		return file;
	}

	public void setFile(SharedFile file) {
		this.file = file;
	}

	@ManyToOne
	@JoinColumn(nullable=false)
	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}
}
