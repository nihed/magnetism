package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * This is a simple table of people who "want in" on the 
 * stealth mode homepage. It is deliberately kept separate from 
 * EmailResource and all real code, we just want a simple list of 
 * whatever unvalidated text people typed into the box.
 * 
 * @author hp
 *
 */
@Entity
public class WantsIn extends DBUnique {

	private static final long serialVersionUID = 1L;
	private String address;
	private int count;
	private long creationDate; // first request to get in
	private long updateDate;   // latest request to get in
	private boolean invitationSent;
	
	// only called by hibernate
	protected WantsIn() {
		this(null);
	}
	
	public WantsIn(String address) {
		creationDate = -1;
		updateDate = -1;
		setAddress(address);
		setCount(0);
		invitationSent = false;
	}
	
	@Column(unique=true, nullable=false)
	public String getAddress() {
		return address;
	}
	
	// only hibernate calls this
	protected void setAddress(String address) {
		this.address = address;
	}
	
	@Column(nullable=false)
	public int getCount() {
		return count;
	}
	
	protected void setCount(int count) {
		this.count = count;
	}
	
	public void incrementCount() {
		setCount(count + 1);
		setUpdateDate(new Date());
	}
	
	@Column(nullable=false)
	public Date getCreationDate() {
		if (creationDate < 0) {
			creationDate = System.currentTimeMillis();
		}
		return new Date(creationDate);
	}

	// only hibernate should call
	protected void setCreationDate(Date creationDate) {
		this.creationDate = creationDate.getTime();
	}
	
	@Column(nullable=false)
	public Date getUpdateDate() {
		if (updateDate < 0) {
			updateDate = System.currentTimeMillis();
		}
		return new Date(updateDate);
	}

	// only hibernate should call
	protected void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate.getTime();
	}
	
	@Column(nullable=false)
	public boolean isInvitationSent() {
		return invitationSent;
	}
	
	public void setInvitationSent(boolean invitationSent) {
		this.invitationSent = invitationSent;
	}
}
