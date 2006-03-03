package com.dumbhippo.server;

public enum Character {
	MUSIC_GEEK("Melodie", "butterfly@dumbhippo.com"),
	LOVES_ACTIVITY("Haley", "hippo@dumbhippo.com");
	
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
