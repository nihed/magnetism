package com.dumbhippo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.AgeUtils;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PromotionCode;

@Entity
@Table(name="InvitationToken", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"id", "invitee_id"})}
	   )
public class InvitationToken extends Token {
	private static final long serialVersionUID = 1L;
	
	private Resource invitee;
	private Set<InviterData> inviters;
	private boolean viewed;
	private String promotion;
	private User resultingPerson;
	
	/**
	 * When an invitation goes through, a person is created.
	 */
	@OneToOne
	public User getResultingPerson() {
		return resultingPerson;
	}

	public void setResultingPerson(User resultingPerson) {
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
	 * @param inviter InviterData for the inviter who reinitiated the invitation
	 */
	public InvitationToken(InvitationToken original, InviterData inviter) {
		super(true);
		this.viewed = original.viewed;
		this.invitee = original.invitee;
		this.inviters = new HashSet<InviterData>(original.inviters);
        // make sure the creation dates agree
		inviter.setInvitationDate(this.getCreationDate());
        if (!this.inviters.contains(inviter)) {
    		addInviter(inviter);
        }
	}

	@OneToOne
	@JoinColumn(nullable=false)
	public Resource getInvitee() {
		return invitee;
	}

	@OneToMany(fetch=FetchType.EAGER)
	public Set<InviterData> getInviters() {
		return inviters;
	}

	/**
	 * 
	 * @param inviter inviterData for an inviter to be added
	 */
	public void addInviter(InviterData inviter) {
		inviters.add(inviter);
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
	
	public String getPromotion() {
		return promotion;
	}
	
	public void setPromotion(String promotion) {
		this.promotion = promotion;
	}
	
	@Transient
	public void setPromotionCode(PromotionCode promotionCode) {
		if (promotionCode != null)
			setPromotion(promotionCode.getCode());
		else
			setPromotion(null);
	}
	
	@Transient
	public PromotionCode getPromotionCode() {
		if (promotion != null) {
			try {
				return PromotionCode.check(promotion);
			} catch (NotFoundException e) {
			}
		}
		
		return null;
	}
		
	@Override
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
			return 60*60*24*90; // 90 days 
		} else if (invitee instanceof AimResource) {
			return 60*60*24*7; // 7 days
		} else {
			return super.getExpirationPeriodInSeconds();
		}
	}
}
