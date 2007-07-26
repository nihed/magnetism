package com.dumbhippo.server;

public enum Character {
	MUSIC_GEEK("Melodie", "mugshot@mugshot.org"),
	LOVES_ACTIVITY("Haley", "mugshot@mugshot.org"),
	MUGSHOT("Mugshot", "mugshot@mugshot.org"),
	GNOME("GNOME Online", "FIXME@gnome.org");
	
	private String defaultNickname;
	private String email;
	
	private Character(String defaultNickname, String email) {
		this.defaultNickname = defaultNickname;
		this.email = email;
	}
	
	public String getDefaultNickname() {
		return defaultNickname;
	}
	
	public String getEmail() {
		return email;
	}
}
