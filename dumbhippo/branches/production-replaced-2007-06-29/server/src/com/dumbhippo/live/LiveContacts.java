package com.dumbhippo.live;

import java.util.Set;

import com.dumbhippo.identity20.Guid;

public class LiveContacts extends LiveObject {
	private Set<Guid> contacts;

	public LiveContacts(Guid guid) {
		super(guid);
	}
	
	public void setContacts(Set<Guid> contacts) {
		this.contacts = contacts;
	}
	
	public Set<Guid> getContacts() {
		return contacts;
	}
}
