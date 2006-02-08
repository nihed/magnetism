package com.dumbhippo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import com.dumbhippo.AgeUtils;

@Entity
public class InvitationToken extends Token {
	private static final long serialVersionUID = 1L;
	
	private Resource invitee;
	private Set<InviterData> inviters;
	private boolean viewed;
	private Person resultingPerson;
	
	// @PersistenceContext(unitName = "dumbhippo")
	// private EntityManager em;	
	
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
		this.inviters = new HashSet<InviterData>();
	}

	public InvitationToken(Resource invitee, InviterData inviter) {
		super(true);
		this.viewed = false;
		this.invitee = invitee;
		this.inviters = new HashSet<InviterData>();
		/*
		if (inviter != null) {
			InviterData inviterData = 
				new InviterData(inviter, this.getCreationDate().getTime());
			em.persist(inviterData);
			this.inviters.add(inviterData);
		}
		*/
		if (inviter != null) {
			inviter.setInvitationDate(this.getCreationDate());
			addInviter(inviter);
		}		
		
	}

	/**
	 * Copy an invitation token with a new auth key and creation date, usually
	 * because the old one was expired. 
	 * 
	 * @param original the old token
	 * @inviter User who reinitiated the invitation
	 */
	public InvitationToken(InvitationToken original, InviterData inviterData) {
		super(true);
		this.viewed = original.viewed;
		this.invitee = original.invitee;
		this.inviters = new HashSet<InviterData>(original.inviters);
        // make sure the creation dates agree
		inviterData.setInvitationDate(this.getCreationDate());
        if (!this.inviters.contains(inviterData)) {
    		addInviter(inviterData);
        }
        
		// update the invitation dates only in the new set of inviters
        /*
		for (InviterData localInviterData : this.inviters) {
			if (localInviterData.getInviter().equals(inviterData.getInviter())) {
				localInviterData.setInvitationDate(this.getCreationDate());
				localInviterData.setDeleted(false);
				return;
			}
		}
		*/
		// we did not find this inviter in the list of inviters, so should add him


	}

	@OneToOne
	@JoinColumn(nullable=false,unique=true)
	public Resource getInvitee() {
		return invitee;
	}
	
    /*
	@ManyToMany(fetch=FetchType.EAGER)
	public Set<User> getInviters() {
		return inviters;
	}
	*/

	@OneToMany(fetch=FetchType.EAGER)
	public Set<InviterData> getInviters() {
		return inviters;
	}

	/**
	 * 
	 * @param inviterData inviter data to be added
	 */
	public void addInviter(InviterData inviterData) {
		inviters.add(inviterData);
	}
    
	protected void setInvitee(Resource invitee) {
		this.invitee = invitee;
	}

	protected void setInviters(Set<InviterData> inviters) {
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
	public String getHumanReadableString() {
		return getHumanReadableInvitee() + " " + getHumanReadableAge();
	}
	
	@Transient
	public String getHumanReadableInvitee() {
		return invitee.getHumanReadableString();
	}
	
	@Transient
	public String getHumanReadableAge() {
		// age is in seconds
        long age = (System.currentTimeMillis() - getCreationDate().getTime()) / 1000;
        return "sent " + AgeUtils.formatAge(age);  
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
