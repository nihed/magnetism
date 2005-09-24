package com.dumbhippo.server;

import org.hibernate.Session;

import com.dumbhippo.persistence.Storage;

public class InvitationSystemBean implements InvitationSystem {

	public String createInvitationKey(Account inviter, String aimAddress) {
		Session hsession = Storage.getGlobalPerThreadSession().getSession();
		Person invitee = (Person) hsession.createQuery("from Person p, ");
		// FIXME
		return null;
	}
	
}
