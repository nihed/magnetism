package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.identity20.RandomToken;

@Entity
@Table(name="ResourceClaimToken", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"person_id", "resource_id"})}
	      )
public class ResourceClaimToken extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private Person person;
	private Resource resource;
	private String authKey;
	private Date creationDate;
	
	protected ResourceClaimToken() {
	}
	
	public ResourceClaimToken(Person person, Resource resource) {
		this.person = person;
		this.resource = resource;
		this.authKey = RandomToken.createNew().toString();
		this.creationDate = new Date(System.currentTimeMillis());
	}
	
	@Column(nullable=false)
	public String getAuthKey() {
		return authKey;
	}
	protected void setAuthKey(String authKey) {
		this.authKey = authKey;
	}
	
	@ManyToOne
	@Column(nullable=false)
	public Person getPerson() {
		return person;
	}
	protected void setPerson(Person person) {
		this.person = person;
	}
	
	// this is nullable because sometimes the resource comes in 
	// along with the validation, e.g. we see who we get the IM
	// from and that is the validated AIM resource. The 
	// uniqueness constraint still applies though (only one active
	// "can be used by any resource" verifier at a time)
	@ManyToOne
	@Column(nullable=true)
	public Resource getResource() {
		return resource;
	}
	protected void setResource(Resource resource) {
		this.resource = resource;
	}

	@Column(nullable=false)
	public Date getCreationDate() {
		return creationDate;
	}

	protected void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
}
