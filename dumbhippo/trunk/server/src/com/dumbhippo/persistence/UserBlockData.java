package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
@Table(name="UserBlockData", 
	   uniqueConstraints = {
			@UniqueConstraint(columnNames={"user_id", "block_id"})
		})
public class UserBlockData extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private User user;
	private Block block;
	private long clickedTimestamp;
	private boolean ignored;
	private long ignoredTimestamp;
	private boolean deleted;
	
	public UserBlockData() {
		this.clickedTimestamp = -1;
		this.ignoredTimestamp = -1;
		this.ignored = false;
		this.deleted = false;
	}
	
	public UserBlockData(User user, Block block) {
		this();
		this.block = block;
		this.user = user;
	}
	
	@JoinColumn(nullable=false)
	@ManyToOne
	public User getUser() {
		return user;
	}

	protected void setUser(User user) {
		this.user = user;
	}
	
	@JoinColumn(nullable=false)
	@ManyToOne
	public Block getBlock() {
		return block;
	}

	protected void setBlock(Block block) {
		this.block = block;
	}	
	
	@Column(nullable=true)
	public Date getClickedTimestamp() {
		return clickedTimestamp >= 0 ? new Date(clickedTimestamp) : null;
	}

	@Transient
	public long getClickedTimestampAsLong() {
		return clickedTimestamp;
	}
	
	public void setClickedTimestamp(Date clickedTimestamp) {
		this.clickedTimestamp = clickedTimestamp != null ? clickedTimestamp.getTime() : -1;
	}
	
	public void setClickedTimestampAsLong(long clickedTimestamp) {
		this.clickedTimestamp = clickedTimestamp;
	}
	
	@Transient
	public boolean isClicked() {
		return this.clickedTimestamp >= 0;
	}
	
	@Column(nullable=false)
	public boolean isIgnored() {
		return ignored;
	}	

	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}
	
	@Column(nullable=true)
	public Date getIgnoredTimestamp() {
		return ignoredTimestamp >= 0 ? new Date(ignoredTimestamp) : null;
	}

	@Transient
	public long getIgnoredTimestampAsLong() {
		return ignoredTimestamp;
	}
	
	public void setIgnoredTimestamp(Date ignoredTimestamp) {
		this.ignoredTimestamp = ignoredTimestamp != null ? ignoredTimestamp.getTime() : -1;
	}
	
	public void setIgnoredTimestampAsLong(long ignoredTimestamp) {
		this.ignoredTimestamp = ignoredTimestamp;
	}	

	@Column(nullable=false)
	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}	
	
	@Override
	public String toString() {
		return "{UserBlockData user=" + user + " block=" + block + "}";
	}
	
	// hashCode and equals for this class are intended to ensure uniqueness 
	// by user-block pair when in a set
	@Override
	public int hashCode() {
		int h = 33;
		if (user != null)
			h += user.hashCode();
		if (block != null)
			h += block.hashCode();
		return h;
	}
	
	@Override 
	public boolean equals(Object other) {
		if (!(other instanceof UserBlockData))
			return false;
		UserBlockData otherData = (UserBlockData) other;

		// most common case first
		if (this.user != null && this.block != null) {
			if (otherData.user != null && otherData.block != null)
				return otherData.user.equals(this.user) && otherData.block.equals(this.block);
			else
				return false;
		} else if (this.user == null) {
			return otherData.user == null && otherData.block != null && otherData.block.equals(this.block);
		} else if (this.block == null) {
			return otherData.block == null && otherData.user != null && otherData.user.equals(this.user);
		} else {
			assert this.user == null;
			assert this.block == null;
			return otherData.block == null && otherData.user == null;
		}
	}
}
