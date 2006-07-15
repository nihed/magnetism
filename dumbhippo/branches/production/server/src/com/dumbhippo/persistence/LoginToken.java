package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

@Entity
public class LoginToken extends Token {
	private static final long serialVersionUID = 1L;
	
	private Resource resource;
	
	// hibernate constructor
	protected LoginToken() {
		super(false);
	}
	
	public LoginToken(Resource resource) {
		super(true);
		this.resource = resource;
	}
		
	@OneToOne
	@JoinColumn(nullable=false,unique=true)
	public Resource getResource() {
		return resource;
	}
	protected void setResource(Resource resource) {
		this.resource = resource;
	}

	@Transient
	@Override
	public long getExpirationPeriodInSeconds() {
		if (resource instanceof EmailResource) {
			return 60*60*24*1; // 1 day 
		} else if (resource instanceof AimResource) {
			return 60*30; // half hour 
		} else {
			return super.getExpirationPeriodInSeconds();
		}
	}
	
	@Override
	public String toString() {
		return "{login via " + resource + " token " + super.toString() + "}";
	}
}
