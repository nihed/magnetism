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

	public EmailResource(String string) {
		super();
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
		this.email = email;
	}

	@Override
	@Transient
	public String getHumanReadableString() {
		return getEmail();
	}
}
