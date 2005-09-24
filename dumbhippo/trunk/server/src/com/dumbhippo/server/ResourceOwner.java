/**
 * 
 */
package com.dumbhippo.server;

import com.dumbhippo.persistence.GuidPersistable;


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
interface ResourceOwner extends GuidPersistable {

}
