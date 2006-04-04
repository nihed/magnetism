package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * AccountClaim represents knowledge by the system that an Account
 * owns a particular resource. "Owner" in this context means the user of the
 * account/address, or the publisher of a web site.
 * 
 * CacheConcurrencyStrategy.READ_ONLY is probably not long-term possible,
 * since we'll have to add some way to "merge" accounts or whatever.
 * 
 * @author otaylor
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.READ_ONLY)
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
	public User getOwner() {
		return owner;
	}

	@OneToOne
	@JoinColumn(nullable = false, unique = true)
	public Resource getResource() {
		return resource;
	}

	protected void setOwner(User owner) {
		this.owner = owner;
	}

	protected void setResource(Resource resource) {
		this.resource = resource;
	}
}
