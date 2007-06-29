package com.dumbhippo.hungry.util;

public enum ConfigValue {
	BASEURL("baseurl"),
	DATABASE_URL("database.url"),
	DATABASE_DRIVER("database.driver"),
	DATABASE_USER("database.user"),
	DATABASE_PASSWORD("database.password"),
	JIVE_HOST("jive.host"),
	JIVE_PORT("jive.port");

	private String property;

	private ConfigValue(String property) {
		this.property = property;
	}
	
	public String getProperty() {
		return property;
	}
}
