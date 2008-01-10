package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name="SharedFileUser", 
		   uniqueConstraints = 
			      {@UniqueConstraint(columnNames={"file_id", "user_id"})}
		   )
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class SharedFileUser extends DBUnique {

	private static final long serialVersionUID = 1L;

	private SharedFile file;
	private User user;
	
	protected SharedFileUser() {
	}
	
	public SharedFileUser(SharedFile file, User user) {
		this.file = file;
		this.user = user;
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
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}
