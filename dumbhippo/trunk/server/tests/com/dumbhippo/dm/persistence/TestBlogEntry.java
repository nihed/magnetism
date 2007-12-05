package com.dumbhippo.dm.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class TestBlogEntry extends TestDBUnique {
	private long serial;
	private TestUser user;
	private long timestamp;
	private String title;
	
	public TestBlogEntry(TestUser user, long serial, Date timestamp) {
		this.user = user;
		this.serial = serial;
		this.timestamp = timestamp.getTime();
	}

	protected TestBlogEntry() {
	}

	@Column(nullable=false)
	public long getSerial() {
		return this.serial;
	}
	
	protected void setSerial(long serial) {
		this.serial = serial;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@ManyToOne
	@JoinColumn(nullable=false)
	public TestUser getUser() {
		return user;
	}

	protected void setUser(TestUser user) {
		this.user = user;
	}
}
