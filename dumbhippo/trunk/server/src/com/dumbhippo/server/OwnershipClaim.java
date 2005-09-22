/**
 * 
 */
package com.dumbhippo.server;

/**
 * OwnershipClaim represents a claim asserted by some ResourceOwner that some other
 * ResourceOwner is the owner of a particular resource. "Owner" in this context
 * means the user of the account/address, or the publisher of a web site.
 * The owner might be a person or a group or just an ID we made up that has
 * no information associated with it other than its ID-ness.
 * 
 * @author hp
 *
 */
final class OwnershipClaim {
	private ResourceOwner claimedOwner;
	private Resource resource;
	private ResourceOwner assertedBy;
	
	ResourceOwner getAssertedBy() {
		return assertedBy;
	}
	
	void setAssertedBy(ResourceOwner assertedBy) {
		this.assertedBy = assertedBy;
	}
	
	ResourceOwner getClaimedOwner() {
		return claimedOwner;
	}
	
	void setClaimedOwner(ResourceOwner claimedOwner) {
		this.claimedOwner = claimedOwner;
	}
	
	Resource getResource() {
		return resource;
	}
	
	void setResource(Resource resource) {
		this.resource = resource;
	}
}
