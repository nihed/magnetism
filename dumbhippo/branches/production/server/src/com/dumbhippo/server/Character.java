package com.dumbhippo.server;

public enum Character {
	/* We map all of these to the same character because we only have one
	 * user-visible one now.  Possibly we could delete this class entirely.
	 */
	MUSIC_GEEK("Melodie", "mugshot@mugshot.org"),
	LOVES_ACTIVITY("Haley", "mugshot@mugshot.org"),
	MUGSHOT("Mugshot", "mugshot@mugshot.org");	
	
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
