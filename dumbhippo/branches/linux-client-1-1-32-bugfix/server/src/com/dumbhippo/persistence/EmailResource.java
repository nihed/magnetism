/**
 * 
 */
package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;


/**
 * @author hp
 *
 */
@Entity
public class EmailResource extends Resource {
	
	private static final Logger logger = GlobalSetup.getLogger(EmailResource.class); 
	
	private static final long serialVersionUID = 0L;
	
	private String email;
	
	/**
	 * Validate and canonicalize (e.g. for case and leading/trailing space) the email address.
	 * 
	 * @param str the name to validate and normalize
	 * @return normalized version
	 * @throws ValidationException
	 */
	public static String canonicalize(String str) throws ValidationException {
		if (str == null)
			return null;
		str = str.trim();
		if (str.length() == 0)
			throw new ValidationException("Email address is empty");
		int at = str.indexOf('@');
		if (at < 0)
			throw new ValidationException("No @ sign in email address");
		if (at == 0)
			throw new ValidationException("Email address has nothing before the @");
		if (str.substring(at + 1).indexOf('@') >= 0)
			throw new ValidationException("Email address has two @ signs");
		
		// the domain is not case-sensitive
		StringBuffer sb = new StringBuffer();
		sb.append(str.substring(0, at + 1));
		sb.append(str.substring(at + 1).toLowerCase());
		return sb.toString();
	}
	
	protected EmailResource() {}

	// this is a "last ditch" validation to protect the DB, we should have
	// checked before here to come up with a user-visible message
	private void validateEmail(String str) throws IllegalArgumentException {
		try {
			canonicalize(str);
		} catch (ValidationException e) {
			// log this since it doesn't always make it out of the layers of ejb/hibernate fun
			logger.error("Invalid email address '" + str + "': " + e.getMessage());
			throw new IllegalArgumentException("Invalid email address '" + str + "': " + e.getMessage(), e);
		}
	}
	
	public EmailResource(String string) {
		setEmail(string);
	}

	@Column(unique=true, nullable=false)
	public String getEmail() {
		return email;
	}
	
	@Transient
	public String getEncodedEmail() {
		return StringUtils.urlEncode(email);
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
