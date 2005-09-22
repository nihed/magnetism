/**
 * 
 */
package com.dumbhippo.server;

/**
 * 
 * The ResourceOwner is just an arbitrary ID that can own resources.
 * Ownership is defined by an OwnershipClaim. The ResourceOwner might
 * end up having subclasses such as Person, Group, or Contact.
 * Contact being "person that we only know since they are a contact
 * of someone we do know," for example "John according to George's address book"
 * might be a Contact. For now not bothering with subclasses since it
 * isn't clear which we want.
 * 
 * Another likely subclass might be TheMan/TheSystem/Superuser, which is 
 * an owner we can always trust to make a correct OwnershipClaim.
 * 
 * @author hp
 *
 */
final class ResourceOwner {
	private Guid guid;

	ResourceOwner() {
		guid = Guid.createNew();
	}
	
	ResourceOwner(Guid guid) {
		this.guid = guid;
	}

	Guid getGuid() {
		return guid;
	}
	
	/** 
	 * Private because only Hibernate should change this.
	 * @param guid
	 */
	private void setGuid(Guid guid) {
		this.guid = guid;
	}
}
