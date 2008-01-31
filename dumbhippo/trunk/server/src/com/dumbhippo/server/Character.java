package com.dumbhippo.server;

import com.dumbhippo.persistence.AccountType;

public enum Character {
	MUSIC_GEEK("Melodie", "mugshot@mugshot.org", "nobody@mugshot.org", AccountType.MUGSHOT),
	LOVES_ACTIVITY("Haley", "mugshot@mugshot.org", "nobody@mugshot.org", AccountType.MUGSHOT),
	MUGSHOT("Mugshot", "mugshot@mugshot.org", "nobody@mugshot.org", AccountType.MUGSHOT),
	GNOME("GNOME Online", "online@gnome.org", "noreply@gnome.org", AccountType.GNOME);
	
	private String defaultNickname;
	private String email;
	private String replyToEmail;
	private AccountType accountType;
	
	private Character(String defaultNickname, String email, String replyToEmail, AccountType accountType) {
		this.defaultNickname = defaultNickname;
		this.email = email;
		this.replyToEmail = replyToEmail;
		this.accountType = accountType;
	}
	
	public String getDefaultNickname() {
		return defaultNickname;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getReplyToEmail() {
		return replyToEmail;
	}
	
	public AccountType getAccountType() {
	    return accountType;	
	}
}
