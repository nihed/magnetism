package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;


@Entity
public class Administrator extends DBUnique {

	private static final long serialVersionUID = 0L;

	private Account account;
	
	protected Administrator() { 
	}

	@OneToOne
	@JoinColumn(nullable=false)
	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}
}
