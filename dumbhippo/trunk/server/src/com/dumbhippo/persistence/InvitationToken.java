package com.dumbhippo.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

@Entity
public class InvitationToken extends Token {
	private static final long serialVersionUID = 1L;
	
	private Resource invitee;
	private Set<User> inviters;
	private boolean viewed;
	private Person resultingPerson;
	
	/**
	 * When an invitation goes through, a person is created.
	 */
	// FIXME is OneToOne correct?
	// FIXME should be User
	@OneToOne
	public Person getResultingPerson() {
		return resultingPerson;
	}

	public void setResultingPerson(Person resultingPerson) {
		this.resultingPerson = resultingPerson;
	}

	// for hibernate to use
	protected InvitationToken() {
		super(false);
		this.inviters = new HashSet<User>();
	}

	public InvitationToken(Resource invitee, User inviter) {
		super(true);
		this.viewed = false;
		this.invitee = invitee;
		this.inviters = new HashSet<User>();
		if (inviter != null)
			this.inviters.add(inviter);
	}

	/**
	 * Copy an invitation token with a new auth key and creation date, usually
	 * because the old one was expired.
	 * 
	 * @param original the old token
	 */
	public InvitationToken(InvitationToken original) {
		super(true);
		this.viewed = original.viewed;
		this.invitee = original.invitee;
		this.inviters = new HashSet<User>(original.inviters);
	}
	
	@OneToOne
	@JoinColumn(nullable=false,unique=true)
	public Resource getInvitee() {
		return invitee;
	}

	@ManyToMany(fetch=FetchType.EAGER)
	public Set<User> getInviters() {
		return inviters;
	}

	public void addInviter(User inviter) {
		if (!inviters.contains(inviter)) {
			inviters.add(inviter);
			// extend expiration period if not expired; normally 
			// the caller will have already checked isExpired() 
			// and created a new token, so this is just paranoia
			// to be sure we don't get a super-old invitation 
			// reactivated
			if (!isExpired()) {
				setCreationDate(new Date());
			}
		}
	}

	protected void setInvitee(Resource invitee) {
		this.invitee = invitee;
	}

	protected void setInviters(Set<User> inviters) {
		if (inviters == null)
			throw new IllegalArgumentException("null");
		this.inviters = inviters;
	}

	@Column(nullable=false)
	public boolean isViewed() {
		return viewed;
	}

	public void setViewed(boolean viewed) {
		this.viewed = viewed;
	}	
		
	public String toString() {
		return "{InvitationToken invitee " + invitee + " token " + super.toString() + "}";
	}

	@Transient
	@Override
	public long getExpirationPeriodInSeconds() {
		if (invitee instanceof EmailResource) {
			return 60*60*24*7; // 7 days 
		} else if (invitee instanceof AimResource) {
			return 60*60*24; // 1 day
		} else {
			return super.getExpirationPeriodInSeconds();
		}
	}
}
