package com.dumbhippo.persistence;

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
	@OneToOne
	public Person getResultingPerson() {
		return resultingPerson;
	}

	public void setResultingPerson(Person resultingPerson) {
		this.resultingPerson = resultingPerson;
	}

	private void initMissing() {
		if (inviters == null)
			inviters = new HashSet<User>();
	}
	
	protected InvitationToken() {
		initMissing();
	}

	public InvitationToken(Resource invitee, User inviter) {
		super(true);
		this.viewed = false;
		this.invitee = invitee;
		this.inviters = new HashSet<User>();
		this.inviters.add(inviter);
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
		this.inviters.add(inviter);
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
