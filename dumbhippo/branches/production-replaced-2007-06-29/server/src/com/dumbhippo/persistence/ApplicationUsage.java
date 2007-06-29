package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Index;

@Entity
@org.hibernate.annotations.Table(appliesTo = "ApplicationUsage", indexes={ 
		@Index(name="userApplication_index", columnNames = { "user_id", "application_id", "date" } ),
		@Index(name="userDate_index", columnNames = { "user_id", "date" } ) 
})
public class ApplicationUsage extends DBUnique {
	private Application application;
	private User user;
	private long date;
	
	public ApplicationUsage() {
	}
	
	public ApplicationUsage(User user, Application application, Date date) {
		this.user = user;
		this.application = application;
		this.date = date.getTime();
	}
	
	@JoinColumn(nullable = false)
	@ManyToOne
	public Application getApplication() {
		return application;
	}
	
	public void setApplication(Application application) {
		this.application = application;
	}

	@Column(nullable = false)
	public Date getDate() {
		return new Date(date);
	}
	
	public void setDate(Date date) {
		this.date = date.getTime();
	}
	
	@JoinColumn(nullable = false)
	@ManyToOne
	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
}
