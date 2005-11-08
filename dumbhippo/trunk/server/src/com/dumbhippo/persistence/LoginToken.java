package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Entity
public class LoginToken extends Token {
	private static final long serialVersionUID = 1L;
	
	private Resource resource;
	
	protected LoginToken() {
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
}
