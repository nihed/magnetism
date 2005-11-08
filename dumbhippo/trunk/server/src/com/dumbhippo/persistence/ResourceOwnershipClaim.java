/**
 * 
 */
package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;


/**
 * ResourceOwnershipClaim represents a claim asserted by some Person that some other
 * Person is the owner of a particular resource. "Owner" in this context
 * means the user of the account/address, or the publisher of a web site.
 * The owner might be a person or a group or just an ID we made up that has
 * no information associated with it other than its ID-ness. Each owner can
 * make only one assertion about a particular resource.
 * 
 * @author hp
 *
 */
@Entity
@Table(name="ResourceOwnershipClaim", 
	   uniqueConstraints = 
	      {@UniqueConstraint(columnNames={"resource_id", "assertedBy_id"})}
      )
public class ResourceOwnershipClaim extends DBUnique {
	private static final long serialVersionUID = 1L;
	private Person claimedOwner;
	private Resource resource;
	private Person assertedBy;
	
	protected ResourceOwnershipClaim() {super();}
	
	public ResourceOwnershipClaim(Person claimedOwner, Resource resource, Person assertedBy) {
		this.claimedOwner = claimedOwner;
		this.resource = resource;
		this.assertedBy = assertedBy;	
	}

	@ManyToOne
	@JoinColumn(nullable=false)
	Person getAssertedBy() {
		return assertedBy;
	}

	@ManyToOne
	@JoinColumn(nullable=false)
	Person getClaimedOwner() {
		return claimedOwner;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	Resource getResource() {
		return resource;
	}

	protected void setAssertedBy(Person assertedBy) {
		this.assertedBy = assertedBy;
	}

	protected void setClaimedOwner(Person claimedOwner) {
		this.claimedOwner = claimedOwner;
	}

	protected void setResource(Resource resource) {
		this.resource = resource;
	}
}
