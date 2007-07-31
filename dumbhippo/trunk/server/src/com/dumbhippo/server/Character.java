package com.dumbhippo.server;

public enum Character {
	MUSIC_GEEK("Melodie", "mugshot@mugshot.org", "nobody@mugshot.org"),
	LOVES_ACTIVITY("Haley", "mugshot@mugshot.org", "nobody@mugshot.org"),
	MUGSHOT("Mugshot", "mugshot@mugshot.org", "nobody@mugshot.org"),
	GNOME("GNOME Online", "online@gnome.org", "noreply@gnome.org");
	
	private String defaultNickname;
	private String email;
	private String replyToEmail;
	
	private Character(String defaultNickname, String email, String replyToEmail) {
		this.defaultNickname = defaultNickname;
		this.email = email;
		this.replyToEmail = replyToEmail;
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
}
