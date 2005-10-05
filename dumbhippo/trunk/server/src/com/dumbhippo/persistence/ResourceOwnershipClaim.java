/**
 * 
 */
package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;


/**
 * ResourceOwnershipClaim represents a claim asserted by some Person that some other
 * Person is the owner of a particular resource. "Owner" in this context
 * means the user of the account/address, or the publisher of a web site.
 * The owner might be a person or a group or just an ID we made up that has
 * no information associated with it other than its ID-ness.
 * 
 * @author hp
 *
 */
@Entity
public class ResourceOwnershipClaim extends DBUnique {
	private Person claimedOwner;
	private Resource resource;
	private Person assertedBy;
	
	protected ResourceOwnershipClaim() {super();}
	
	public ResourceOwnershipClaim(Person claimedOwner, Resource resource) {
		this(claimedOwner, resource, null);
	}
	
	public ResourceOwnershipClaim(Person claimedOwner, Resource resource, Person assertedBy) {
		this.claimedOwner = claimedOwner;
		this.resource = resource;
		this.assertedBy = assertedBy;	
	}

	@ManyToOne
	Person getAssertedBy() {
		return assertedBy;
	}

	@ManyToOne
	Person getClaimedOwner() {
		return claimedOwner;
	}
	
	@ManyToOne
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
