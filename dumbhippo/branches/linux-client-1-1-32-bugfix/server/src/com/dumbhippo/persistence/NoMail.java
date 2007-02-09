package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * Used to store people who've asked us not to send them 
 * email. This is only used for people who do not have accounts.
 * 
 * @author hp
 */
@Entity
public class NoMail extends DBUnique {
	private static final long serialVersionUID = 1L;

	private EmailResource email;
	private boolean mailEnabled;

	public NoMail() {
		this(null);
	}
	
	public NoMail(EmailResource email) {
		setEmail(email);
	}
	
	@OneToOne
	@JoinColumn(nullable = false, unique = true)
	public EmailResource getEmail() {
		return email;
	}

	protected void setEmail(EmailResource email) {
		this.email = email;
	}
	
	@Column(nullable=false)
	public boolean getMailEnabled() {
		return mailEnabled;
	}
	
	public void setMailEnabled(boolean mailEnabled) {
		this.mailEnabled = mailEnabled;
	}
}
