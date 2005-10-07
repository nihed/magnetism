package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.InvitableResource;
import com.dumbhippo.persistence.Invitation;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.InvitationSystemRemote;

@Stateless
public class InvitationSystemBean implements InvitationSystem, InvitationSystemRemote {

	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;

	@EJB
	private transient AccountSystem accounts;
	
	protected Invitation lookupInvitationFor(Resource invitee) {
		Invitation ret;
		try {
			ret = (Invitation) em.createQuery(
				"from Invitation as iv where iv.invitee = :resource")
				.setParameter("resource", invitee).getSingleResult();
		} catch (EntityNotFoundException e) {
			ret = null;
		}
		return ret;
	}

	public Invitation createGetInvitation(Person inviter, Resource invitee) {
		Invitation iv = lookupInvitationFor(invitee);
		if (iv == null) {
			iv = new Invitation(invitee, inviter);
			em.persist(iv);
		} else {
			iv.addInviter(inviter);
		}

		return iv;
	}

	public void sendEmailNotification(Invitation invite, Person inviter) {
		InvitableResource invitee = (InvitableResource) invite.getInvitee();
		invitee.sendInvite(null, invite, inviter);
	}

	public Invitation lookupInvitationByKey(String authKey) {
		Invitation ret;
		try {
			ret = (Invitation) em.createQuery(
				"from Invitation as iv where iv.authKey = :key")
				.setParameter("key", authKey).getSingleResult();
		} catch (EntityNotFoundException e) {
			ret = null;
		}
		return ret;
	}

	protected void notifyInvitationViewed(Invitation invite) {
		// adding @suppresswarnings here makes javac crash, whee
		for (Person inviter : invite.getInviters()) {
			// TODO send notification via xmpp to inviter that invitee viewed
		}
	}
	
	public HippoAccount viewInvitation(Invitation invite) {
		if (invite.isViewed()) {
			throw new IllegalArgumentException("Invitation " + invite + "has already been viewed");
		}
		invite.setViewed(true);
		notifyInvitationViewed(invite);
		Resource invitationResource = invite.getInvitee();
		return accounts.createAccountFromResource(invitationResource);
	}
}
