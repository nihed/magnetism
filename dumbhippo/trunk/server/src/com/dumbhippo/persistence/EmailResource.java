/**
 * 
 */
package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;


/**
 * @author hp
 *
 */
@Entity
public class EmailResource extends Resource {
	
	private static final long serialVersionUID = 0L;
	
	private String email;
	
	protected EmailResource() {}

	// this is a "last ditch" validation to protect the DB, we should have
	// checked before here to come up with a user-visible message
	private void validateEmail(String str) throws IllegalArgumentException {
		// the @ sign check would also catch length == 0, but just trying to be more clear
		if (str.length() == 0) {
			throw new IllegalArgumentException("Empty email address: " + str);
		} else if (!str.contains("@")) {
			throw new IllegalArgumentException("No @ sign in email address: " + str);
		}
	}
	
	public EmailResource(String string) {
		setEmail(string);
	}

	@Column(unique=true, nullable=false)
	public String getEmail() {
		return email;
	}
	
	
	/**
	 * This is protected so only the container calls it. 
	 * This is because EmailResource is treated as immutable,
	 * i.e. once a GUID-EmailAddress pair exists, we never 
	 * change the address associated with that GUID. 
	 * So you don't want to setEmail(). Instead, create
	 * a new EmailResource with the new email.
	 * 
	 * @param email
	 */
	protected void setEmail(String email) {
		if (email != null) {
			validateEmail(email); // in theory the database has nothing that would fail...
			email = email.trim();
		}
		this.email = email;
	}

	@Override
	@Transient
	public String getHumanReadableString() {
		return getEmail();
	}
	
	@Override
	@Transient
	public String getDerivedNickname() {
		String withAt = getEmail();
		int atIndex = withAt.indexOf('@');
		if (atIndex > 0) {
			return withAt.substring(0, atIndex);
		} else {
			return withAt; // something invalid, maybe nothing before the @
		}
	}
}
