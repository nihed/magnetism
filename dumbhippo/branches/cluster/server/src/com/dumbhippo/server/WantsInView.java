package com.dumbhippo.server;

import java.util.Date;

public class WantsInView {

	private String address;
	private int count;
	private Date creationDate;
	
	public WantsInView() {
	}

	public WantsInView(String address, int count, Date creationDate) {
		this.address = address;		
		this.count = count;
		this.creationDate = creationDate;
	}
	
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
}
