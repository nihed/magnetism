package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="CurrentTrack", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"user_id"})}
	      )
public class CurrentTrack extends DBUnique {

	private static final long serialVersionUID = 1L;

	private User user;
	private Track track;
	private long lastUpdated;
	
	protected CurrentTrack() {
		
	}
	
	public CurrentTrack(User user, Track track) {
		this.user = user;
		this.track = track;
	}
	
	@Column(nullable=false)
	public Date getLastUpdated() {
		return new Date(lastUpdated);
	}
	
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated.getTime();
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Track getTrack() {
		return track;
	}
	public void setTrack(Track track) {
		this.track = track;
	}
	
	@OneToOne
	@JoinColumn(nullable=false)
	public User getUser() {
		return user;
	}
	protected void setUser(User user) {
		this.user = user;
	}
}
