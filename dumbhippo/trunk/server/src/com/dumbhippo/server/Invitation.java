package com.dumbhippo.server;

import java.util.HashSet;
import java.util.Set;

import com.dumbhippo.persistence.Resource;

public class Invitation extends Resource {
	private Person invitee;
	private Set<Account> inviters;
	
	public Invitation(Person invitee, Account inviter) {
		this.invitee = invitee;
		this.inviters = new HashSet<Account>();
		this.inviters.add(inviter);
	}

	public Person getInvitee() {
		return invitee;
	}

	public Set<Account> getInviters() {
		return inviters;
	}
}
