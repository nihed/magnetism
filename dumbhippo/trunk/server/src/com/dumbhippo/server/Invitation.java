package com.dumbhippo.server;

import java.util.HashSet;
import java.util.Set;

import com.dumbhippo.persistence.DBUnique;

public class Invitation extends DBUnique {
	public static final String invitationLandingURLPrefix = "http://dumbhippo.com/newuser?auth=";
	
	private Resource invitee;
	private Set<Person> inviters;
	private String authKey;
	
	protected Invitation() {}
	
	public Invitation(Resource invitee, Person inviter) {
		this.invitee = invitee;
		this.inviters = new HashSet<Person>();
		this.inviters.add(inviter);
		authKey = AuthKey.createNew().toString();
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
	
	public String generateAuthURL() {
		return invitationLandingURLPrefix + getAuthKey();
	}
}
