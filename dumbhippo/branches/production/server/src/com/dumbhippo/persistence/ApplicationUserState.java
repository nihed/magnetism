package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="ApplicationUserState", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"application_id", "user_id"})}
	      )
public class ApplicationUserState extends DBUnique {
	private Application application;
	private User user;
	private boolean pinned;
	
	public ApplicationUserState() {
	}
	
	public ApplicationUserState(User user, Application application) {
		this.user = user;
		this.application = application;
	}
	
	@JoinColumn(nullable = false)
	@ManyToOne
	public Application getApplication() {
		return application;
	}
	
	public void setApplication(Application application) {
		this.application = application;
	}
	
	@JoinColumn(nullable = false)
	@ManyToOne
	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
	
	@Column(nullable = false)
	public boolean getPinned() {
		return pinned;
	}
	
	public void setPinned(boolean pinned) {
		this.pinned = pinned;
	}
}
