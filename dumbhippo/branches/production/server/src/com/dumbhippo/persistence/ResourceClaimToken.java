package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="ResourceClaimToken", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"user_id", "resource_id"})}
	      )
public class ResourceClaimToken extends Token {
	private static final long serialVersionUID = 1L;
	
	private User user;
	private Resource resource;
	
	// hibernate constructor
	protected ResourceClaimToken() {
		super(false);
	}
	
	public ResourceClaimToken(User user, Resource resource) {
		super(true);
		this.user = user;
		this.resource = resource;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public User getUser() {
		return user;
	}
	protected void setUser(User user) {
		this.user = user;
	}
	
	// this is nullable because sometimes the resource comes in 
	// along with the validation, e.g. we see who we get the IM
	// from and that is the validated AIM resource. The 
	// uniqueness constraint still applies though (only one active
	// "can be used by any resource" verifier at a time)
	@ManyToOne
	@JoinColumn(nullable=true)
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
			return 60*60*24*3; // 3 days 
		} else if (resource instanceof AimResource) {
			return 60*30; // half hour
		} else {
			return super.getExpirationPeriodInSeconds();
		}
	}
	
	@Override
	public String toString() {
		return "{" + user + " claims " + resource + " token " + super.toString() + "}";
	}
}
