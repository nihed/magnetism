package com.dumbhippo.aimbot;

import com.dumbhippo.aim.ScreenName;

class BotConfig {
	private ScreenName name;
	private String pass;
	
	BotConfig(ScreenName name, String pass) {
		this.name = name;
		this.pass = pass;
	}

	public ScreenName getName() {
		return name;
	}

	public String getPass() {
		return pass;
	}
}
