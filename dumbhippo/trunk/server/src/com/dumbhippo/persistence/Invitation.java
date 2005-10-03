package com.dumbhippo.persistence;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.dumbhippo.identity20.RandomToken;

public class Invitation extends DBUnique {
	private Resource invitee;
	private Set<Person> inviters;
	private String authKey;
	
	protected Invitation() {}
	
	public Invitation(Resource invitee, Person inviter) {
		this.invitee = invitee;
		this.inviters = new HashSet<Person>();
		this.inviters.add(inviter);
		authKey = RandomToken.createNew().toString();
	}

	public Resource getInvitee() {
		return invitee;
	}

	public Set<Person> getInviters() {
		return inviters;
	}

	public String getAuthKey() {
		return authKey;
	}

	public void addInviter(Person inviter) {
		this.inviters.add(inviter);
	}
	
	public String getPartialAuthURL() {
		return "invite/landing?auth=" + getAuthKey();
	}
	
	public String getAuthURL(URL prefix) {
		URL authURL;
		try {
			authURL = new URL(prefix, getPartialAuthURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return authURL.toString();
	}
}
