package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@Table(name="UserBlockData", 
	   uniqueConstraints = {
			@UniqueConstraint(columnNames={"user_id", "block_id"})
		})
public class UserBlockData extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private User user;
	private Block block;
	private long clickedDate;
	private boolean ignored;
	private long ignoredDate;
	
	public UserBlockData() {
		this.clickedDate = -1;
		this.ignoredDate = -1;
		this.ignored = false;
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
	public Date getClickedDate() {
		return clickedDate >= 0 ? new Date(clickedDate) : null;
	}

	public void setClickedDate(Date clickedDate) {
		this.clickedDate = clickedDate != null ? clickedDate.getTime() : -1;
	}	
	
	@Column(nullable=false)
	public boolean isIgnored() {
		return ignored;
	}	

	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}
	
	@Column(nullable=true)
	public Date getIgnoredDate() {
		return ignoredDate >= 0 ? new Date(ignoredDate) : null;
	}

	public void setIgnoredDate(Date ignoredDate) {
		this.ignoredDate = ignoredDate != null ? ignoredDate.getTime() : -1;
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
