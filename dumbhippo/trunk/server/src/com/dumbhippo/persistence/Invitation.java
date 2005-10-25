package com.dumbhippo.persistence;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import com.dumbhippo.identity20.RandomToken;

@Entity
public class Invitation extends DBUnique {
	private static final long serialVersionUID = 1L;
	private Resource invitee;
	private Set<Person> inviters;
	private String authKey;
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
			inviters = new HashSet<Person>();
	}
	
	protected Invitation() {
		initMissing();
	}

	public Invitation(Resource invitee, Person inviter) {
		this.viewed = false;
		this.invitee = invitee;
		this.inviters = new HashSet<Person>();
		this.inviters.add(inviter);
		authKey = RandomToken.createNew().toString();
	}

	@OneToOne
	public Resource getInvitee() {
		return invitee;
	}

	@ManyToMany(fetch=FetchType.EAGER)
	public Set<Person> getInviters() {
		return inviters;
	}

	public String getAuthKey() {
		return authKey;
	}

	public void addInviter(Person inviter) {
		this.inviters.add(inviter);
	}

	protected void setAuthKey(String authKey) {
		this.authKey = authKey;
	}

	protected void setInvitee(Resource invitee) {
		this.invitee = invitee;
	}

	protected void setInviters(Set<Person> inviters) {
		if (inviters == null)
			throw new IllegalArgumentException("null");
		this.inviters = inviters;
	}

	public boolean isViewed() {
		return viewed;
	}

	public void setViewed(boolean viewed) {
		this.viewed = viewed;
	}	
	
	@Transient
	public String getPartialAuthURL() {
		return "jsf/verify.faces?authKey=" + getAuthKey();
	}
	
	@Transient
	public String getAuthURL(URL prefix) {
		URL authURL;
		try {
			authURL = new URL(prefix, getPartialAuthURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return authURL.toString();
	}
	
	public String toString() {
		return "{Invitation invitee=" + invitee + "}";
	}
}
