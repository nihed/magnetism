/**
 * 
 */
package com.dumbhippo.server;

import com.dumbhippo.persistence.Resource;

/**
 * @author hp
 *
 */
class EmailResource extends Resource {
	private String email;
	
	protected EmailResource() {}

	public EmailResource(String string) {
		super();
		setEmail(string);
	}

	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof EmailResource) {
			return ((EmailResource) arg0).email.equals(this.email);
		}
		return false;
	}
	
	
}
