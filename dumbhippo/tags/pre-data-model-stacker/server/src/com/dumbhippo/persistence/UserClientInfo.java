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
           {@UniqueConstraint(columnNames={"user_id", "platform", "distribution", "version", "architecture"})}
   )
public class UserClientInfo extends DBUnique {
	User user;
	String platform;
	String distribution;
	String version;
	String architecture;
	long lastChecked;
	
	public UserClientInfo() {
	}
	
	public UserClientInfo(User user, String platform, String distribution, String version, String architecture) {
		this.user = user;
		this.platform = platform;
		this.distribution = distribution;
		this.version = version;
		this.architecture = architecture;
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
	
	@Column(nullable = false, length = 32)
	public String getVersion() {
		return version;
	}
	
	protected void setVersion(String version) {
		this.version = version;
	}
	
	@Column(nullable = false, length = 32)
	public String getArchitecture() {
		return architecture;
	}
	
	protected void setArchitecture(String architecture) {
		this.architecture = architecture;
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
