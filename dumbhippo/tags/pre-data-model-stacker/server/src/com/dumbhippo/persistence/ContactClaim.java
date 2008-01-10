package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * ContactClaim represents knowledge by a user that one
 * of their contacts owns a particular resource. "Owner" in this context
 * means the user of the account/address, or the publisher of a web site.
 * 
 * Note that we store ContactClaim slightly denormalized: account
 * duplicates information that can be retrieved from contact. This
 * duplication makes database lookups for contacts of the viewer
 * more efficient and also lets us enforce that every pair of
 * account and resource has only one ContactClaim.
 * 
 * @author otaylor
 */
@Entity
@Table(name="ContactClaim", 
	   uniqueConstraints = 
	      {@UniqueConstraint(columnNames={"account_id", "resource_id"})}
      )
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class ContactClaim extends DBUnique {
	private static final long serialVersionUID = 1L;
	private Contact contact;
	private Resource resource;
	private Account account;
	
	protected ContactClaim() {super();}
	
	public ContactClaim(Contact contact, Resource resource) {
		this.contact = contact;
		this.resource = resource;
		this.account = contact.getAccount();	
	}

	@ManyToOne
	@JoinColumn(nullable=false)
	public Contact getContact() {
		return contact;
	}

	@ManyToOne
	@JoinColumn(nullable=false)
	public Resource getResource() {
		return resource;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Account getAccount() {
		return account;
	}

	protected void setContact(Contact contact) {
		this.contact = contact;
	}

	protected void setResource(Resource resource) {
		this.resource = resource;
	}

	protected void setAccount(Account account) {
		this.account = account;
	}
}
