package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Every time you listen to a song, we add it to this history 
 * and update the lastUpdated. Your current song is the row
 * with the latest lastUpdated.
 * 
 * @author hp
 */
@Entity
@Table(name="TrackHistory", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"user_id","track_id"})}
	      )
public class TrackHistory extends DBUnique {

	private static final long serialVersionUID = 1L;

	private User user;
	private Track track;
	private long lastUpdated;
	private int timesPlayed;
	
	protected TrackHistory() {
		this.timesPlayed = 0;
	}
	
	public TrackHistory(User user, Track track) {
		this();
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
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public User getUser() {
		return user;
	}
	protected void setUser(User user) {
		this.user = user;
	}

	@Column(nullable=false)
	public int getTimesPlayed() {
		return timesPlayed;
	}

	public void setTimesPlayed(int timesPlayed) {
		this.timesPlayed = timesPlayed;
	}
}
