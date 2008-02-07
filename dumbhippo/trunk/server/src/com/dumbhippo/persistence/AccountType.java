package com.dumbhippo.persistence;

public enum AccountType {
    GNOME("GNOME Online"),
    MUGSHOT("Mugshot");
    
    private String name;
	
	private AccountType(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
