package com.dumbhippo.server;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.InvitableResource;
import com.dumbhippo.persistence.Invitation;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;

@Stateless
public class InvitationSystemBean implements InvitationSystem {

	@PersistenceContext(unitName = "dumbhippo")
	protected EntityManager em;
	
	protected Invitation lookupInvitationFor(Resource invitee) {
		return (Invitation) em.createQuery(
				"from Invitation as iv where iv.invitee = :resource")
				.setParameter("resource", invitee).getSingleResult();
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

	public void sendEmailNotification(IdentitySpider spider, Invitation invite, Person inviter) {
		InvitableResource invitee = (InvitableResource) invite.getInvitee();
		invitee.sendInvite(spider, null, invite, inviter);
	}

	public Invitation lookupInvitationByKey(String authKey) {
		return (Invitation) em.createQuery(
				"from Invitation as iv where iv.authKey = :key")
				.setParameter("key", authKey).getSingleResult();		
	}
}
