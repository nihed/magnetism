package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import com.dumbhippo.AgeUtils;

/**
 * Stores data associated with a certain inviter for an invitation.
 * 
 * @author marinaz
 */

@Entity
public class InviterData extends DBUnique {

	private static final long serialVersionUID = 1L;
	
	private User inviter;
	private long invitationDate;
	private String invitationSubject;
	private String invitationMessage;
	private boolean initialInvite;
	private boolean invitationDeducted;
	private boolean deleted;
	
	/**
	 * Used only for Hibernate 
	 */
	protected InviterData() {
		this(null, System.currentTimeMillis(), "", "", false);
	}

	public InviterData(User inviter, String invitationSubject, String invitationMessage, boolean invitationDeducted) {	
		this(inviter, System.currentTimeMillis(), invitationSubject, invitationMessage, invitationDeducted);
	}
	
	public InviterData(User inviter, long invitationDate, String invitationSubject, String invitationMessage, boolean invitationDeducted) {	
        this.inviter = inviter;
		this.initialInvite = true;
        this.invitationDate = invitationDate;
        this.invitationSubject = invitationSubject;
        this.invitationMessage = invitationMessage;
        this.invitationDeducted = invitationDeducted;
        this.deleted = false;
	}

	@JoinColumn(nullable=false)
	@ManyToOne
	public User getInviter() {
		return inviter;
	}	
	
	public void setInviter(User inviter) {
		this.inviter = inviter;
	}
	
	@Column(nullable=false)
	public Date getInvitationDate() {
		return new Date(invitationDate);
	}

	public void setInvitationDate(Date invitationDate) {
		this.invitationDate = invitationDate.getTime();
	}
	
	@Column(nullable=false)
	public String getInvitationSubject() {
		return invitationSubject;
	}
	public void setInvitationSubject(String invitationSubject) {
		this.invitationSubject = invitationSubject;
	}
	
	@Column(nullable=false)
	public String getInvitationMessage() {
		return invitationMessage;
	}
	public void setInvitationMessage(String invitationMessage) {
		this.invitationMessage = invitationMessage;
	}
	
	@Column(nullable=false)
	public boolean isInitialInvite() {
		return initialInvite;
	}

	public void setInitialInvite(boolean initialInvite) {
		this.initialInvite = initialInvite;
	}
	
	@Column(nullable=false)  
	public boolean isInvitationDeducted() {
		return invitationDeducted;
	}

	public void setInvitationDeducted(boolean invitationDeducted) {
		this.invitationDeducted = invitationDeducted;
	}
	
	@Column(nullable=false)
	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	@Transient
	public String getHumanReadableAge() {
		// age is in seconds
        long age = (System.currentTimeMillis() - getInvitationDate().getTime()) / 1000;
        if (isInitialInvite()) {
        	return "sent " + AgeUtils.formatAge(age); 
        } else {
        	return "resent " + AgeUtils.formatAge(age);
        }  
	}
}
