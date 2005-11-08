package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import com.dumbhippo.identity20.RandomToken;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Token extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private String authKey;
	
	// store date in this form since it's immutable and lightweight
	private long creationDate;
	
	// constructor for hibernate to use
	protected Token() {
	}
	
	// constructor for subclasses to use, they'll always use initialize=true
	protected Token(boolean initialize) {
		if (initialize) {
			this.authKey = RandomToken.createNew().toString();
			this.creationDate = System.currentTimeMillis();
		}
	}
	
	@Column(nullable=false,unique=true,length=RandomToken.STRING_LENGTH)
	public String getAuthKey() {
		return authKey;
	}
	protected void setAuthKey(String authKey) {
		this.authKey = authKey;
	}

	@Column(nullable=false)
	public Date getCreationDate() {
		return new Date(creationDate);
	}

	protected void setCreationDate(Date creationDate) {
		this.creationDate = creationDate.getTime();
	}
}
