package com.dumbhippo.persistence;

/**
 * The ContactStatus is information that the user has explicitly supplied about
 * a contact. One usage of it is to determine who appears directly in the 
 * online-desktop sidebar, but it could potentially be used for other things
 * later. 
 * 
 * @author otaylor
 */
public enum ContactStatus {
	NONCONTACT, /* Not a contact at all */
	BLOCKED,    /* An actively disliked person */
	COLD,       /* Keep out of the quick list */
	MEDIUM,     /* Auto-choose whether to put in the quick list */
	HOT         /* Always in the quick list */
}
