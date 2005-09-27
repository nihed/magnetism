/**
 * 
 */
package com.dumbhippo.server;

import com.dumbhippo.persistence.DBUnique;
import com.dumbhippo.persistence.Resource;

/**
 * PersonOwnershipClaim represents a claim asserted by some Person that some other
 * Person is the owner of a particular resource. "Owner" in this context
 * means the user of the account/address, or the publisher of a web site.
 * The owner might be a person or a group or just an ID we made up that has
 * no information associated with it other than its ID-ness.
 * 
 * @author hp
 *
 */
class PersonOwnershipClaim extends DBUnique {
	private Person claimedOwner;
	private Resource resource;
	private Person assertedBy;
	
	protected PersonOwnershipClaim() {super();}
	
	public PersonOwnershipClaim(Person claimedOwner, Resource resource) {
		this(claimedOwner, resource, null);
	}
	
	public PersonOwnershipClaim(Person claimedOwner, Resource resource, Person assertedBy) {
		this.claimedOwner = claimedOwner;
		this.resource = resource;
		this.assertedBy = assertedBy;	
	}

	Person getAssertedBy() {
		return assertedBy;
	}

	Person getClaimedOwner() {
		return claimedOwner;
	}
	
	Resource getResource() {
		return resource;
	}
}
