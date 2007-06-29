package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

@Entity
public class ToggleNoMailToken extends Token {
	private static final long serialVersionUID = 1L;
	
	private EmailResource email;
	
	// hibernate constructor
	protected ToggleNoMailToken() {
		super(false);
	}
	
	public ToggleNoMailToken(EmailResource email) {
		super(true);
		this.email = email;
	}
		
	@OneToOne
	@JoinColumn(nullable=false,unique=true)
	public EmailResource getEmail() {
		return email;
	}
	
	protected void setEmail(EmailResource email) {
		this.email = email;
	}
	
	public static int getExpirationInDays() {
		return 30;
	}
	
	@Transient
	@Override
	public long getExpirationPeriodInSeconds() {
			return 60*60*24*getExpirationInDays();
	}
	
	@Override
	public String toString() {
		return "{togglenomail for " + email + " token " + super.toString() + "}";
	}
}
