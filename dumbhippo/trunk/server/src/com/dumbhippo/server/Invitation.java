package com.dumbhippo.server;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.dumbhippo.StringUtils;
import com.dumbhippo.persistence.DBUnique;

public class Invitation extends DBUnique {
	private Person invitee;
	private Set<Person> inviters;
	private String authKey;
	
	protected Invitation() {}
	
	public Invitation(Person invitee, Person inviter) {
		this.invitee = invitee;
		this.inviters = new HashSet<Person>();
		this.inviters.add(inviter);
		Random r = new SecureRandom();
		byte[] keyBytes = new byte[10];
		r.nextBytes(keyBytes);
		this.authKey = StringUtils.hexEncode(keyBytes);
	}

	public Person getInvitee() {
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
}
