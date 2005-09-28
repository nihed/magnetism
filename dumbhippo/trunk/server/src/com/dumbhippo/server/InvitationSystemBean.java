package com.dumbhippo.server;

import org.hibernate.Session;

import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;

public class InvitationSystemBean implements InvitationSystem {

	protected Invitation lookupInvitationFor(Resource invitee) {
		Session hsession = Storage.getGlobalPerThreadSession().getSession();
		return (Invitation) hsession.createQuery(
				"from Invitation as iv where iv.invitee = :resource")
				.setParameter("resource", invitee).uniqueResult();
	}

	public Invitation createGetInvitation(Person inviter, Resource invitee) {
		SessionWrapper session = Storage.getGlobalPerThreadSession();
		session.beginTransaction();
		Invitation iv = lookupInvitationFor(invitee);
		if (iv == null) {
			iv = new Invitation(invitee, inviter);
			session.getSession().save(iv);
		} else {
			iv.addInviter(inviter);
		}

		session.commitTransaction();
		return iv;
	}

	public void sendEmailNotification(IdentitySpider spider, Invitation invite, Person inviter) {
		InvitableResource invitee = (InvitableResource) invite.getInvitee();
		invitee.sendInvite(spider, invite, inviter);
	}
}
