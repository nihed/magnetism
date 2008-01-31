package com.dumbhippo;

import com.dumbhippo.persistence.AccountType;

/**
 * 
 * The server can have multiple "flavors" or "skins" with different 
 * hostnames and look&feel, the two right now are GNOME and Mugshot.
 * 
 * We treat XMPP as a third "site" right now, but it may be correct 
 * eventually to require a user connecting to XMPP to specify 
 * one of the other sites.
 *
 */
public enum Site {
	GNOME("GNOME Online", AccountType.GNOME),
	MUGSHOT("Mugshot", AccountType.MUGSHOT),
	XMPP("Mugshot", AccountType.MUGSHOT),
	// this means the code is running from a cron job for example, so 
	// we don't have a meaningful "site" it's coming from. DO NOT just 
	// use this because you're too lazy to pass the GNOME/MUGSHOT 
	// thing down through the call stack.
	NONE("Mugshot", AccountType.MUGSHOT);
	
	private String siteName;
	private AccountType accountType;
	
	private Site(String siteName, AccountType accountType) {
		this.siteName = siteName;
		this.accountType = accountType;
	}
	
	public String getSiteName() {
		return siteName;
	}
	
	public AccountType getAccountType() {
		return accountType;
	}
}
