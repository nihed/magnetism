/**
 * 
 */
package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.dumbhippo.identity20.Guid;

@Entity
public class EmailDetails {
	
	private static final long serialVersionUID = 0L;
	
	private Guid emailGuid;
	
	private boolean googleServicesEnabled;
	
	protected EmailDetails() {}
	
	public EmailDetails(EmailResource email) {
		emailGuid = email.getGuid();
	}

	@Id
	@Column(length = Guid.STRING_LENGTH, nullable=false)
	public String getId() {
		return emailGuid.toString();
	}
	
	protected void setId(String id) {
		emailGuid = Guid.parseTrustedString(id);
	}
	
	@Column(nullable=false)
	public boolean getGoogleServicesEnabled() {
		return googleServicesEnabled;
	}

	public void setGoogleServicesEnabled(boolean b) {
		googleServicesEnabled = b;
	}
}
