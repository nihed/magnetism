package com.dumbhippo.persistence;

/**
 * 
 * Indicates who can see the information from a Post.
 * Note that we stuff this in the database so if you change
 * this enum you will have to migrate the db.
 * 
 * @author hp
 *
 */
public enum PostVisibility {
	ANONYMOUSLY_PUBLIC,	
	RECIPIENTS_ONLY,
	ATTRIBUTED_PUBLIC;
}
