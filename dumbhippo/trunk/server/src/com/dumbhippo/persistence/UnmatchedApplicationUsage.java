package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Index;

/**
 * Represents information about application usage reported by the 
 * client that we couldn't match to any application in our database.
 * The purpose of recording this is to see what applications we are
 * missing, though we could also fill in ApplicationUsage information
 * based on entries in here once an application is added to the database.
 * 
 * @author otaylor
 */
@Entity
@org.hibernate.annotations.Table(appliesTo = "UnmatchedApplicationUsage", indexes={ 
		@Index(name="userWmClass_index", columnNames = { "user_id", "wmClass", "date" } ) 
})

public class UnmatchedApplicationUsage extends DBUnique {
	private String wmClass;
	private User user;
	private long date;
	
	public UnmatchedApplicationUsage() {
	}
	
	public UnmatchedApplicationUsage(User user, String wmClass, Date date) {
		this.user = user;
		this.wmClass = wmClass;
		this.date = date.getTime();
	}
	
	@Column(nullable = true)
	public String getWmClass() {
		return wmClass;
	}
	
	public void setWmClass(String wmClass) {
		this.wmClass = wmClass;
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
