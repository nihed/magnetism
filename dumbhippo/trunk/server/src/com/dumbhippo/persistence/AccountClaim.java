package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

/**
 * AccountClaim represents knowledge by the system that an Account
 * owns a particular resource. "Owner" in this context means the user of the
 * account/address, or the publisher of a web site.
 * 
 * @author otaylor
 */
@Entity
public class AccountClaim extends DBUnique {
	private static final long serialVersionUID = 1L;

	private User owner;

	private Resource resource;

	protected AccountClaim() {
		super();
	}

	public AccountClaim(User owner, Resource resource) {
		this.owner = owner;
		this.resource = resource;
	}

	@ManyToOne
	@JoinColumn(nullable = false)
	User getowner() {
		return owner;
	}

	@OneToOne
	@JoinColumn(nullable = false, unique = true)
	Resource getResource() {
		return resource;
	}

	protected void setOwner(User owner) {
		this.owner = owner;
	}

	protected void setResource(Resource resource) {
		this.resource = resource;
	}
}
