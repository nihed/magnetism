package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="UserClientInfo", 
        uniqueConstraints = 
           {@UniqueConstraint(columnNames={"user_id", "platform", "distribution"})}
   )
public class UserClientInfo extends DBUnique {
	User user;
	String platform;
	String distribution;
	long lastChecked;
	
	public UserClientInfo() {
	}
	
	public UserClientInfo(User user, String platform, String distribution) {
		this.user = user;
		this.platform = platform;
		this.distribution = distribution;
	}

	@Column(nullable = false, length = 32)
	public String getDistribution() {
		return distribution;
	}
	
	protected void setDistribution(String distribution) {
		this.distribution = distribution;
	}
	
	@JoinColumn(nullable = false)
	public Date getLastChecked() {
		return new Date(lastChecked);
	}
	
	public void setLastChecked(Date lastChecked) {
		this.lastChecked = lastChecked.getTime();
	}

	@Column(nullable = false, length = 32)
	public String getPlatform() {
		return platform;
	}
	
	protected void setPlatform(String platform) {
		this.platform = platform;
	}
	
	@JoinColumn(nullable = false)
	@ManyToOne
	public User getUser() {
		return user;
	}
	
	protected void setUser(User user) {
		this.user = user;
	}
}
