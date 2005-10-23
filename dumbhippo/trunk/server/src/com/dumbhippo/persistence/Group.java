package com.dumbhippo.persistence;

import java.util.Collections;
import java.util.Set;

import javax.persistence.ManyToMany;

public class Group extends GuidPersistable {
	private static final long serialVersionUID = 1L;
	private String name;
	private Set<Person> members;
	private boolean markedForDelete;
	
	protected Group() {}
	
	public Group(String name) {
		this.name = name;
	}
		
	protected boolean isMarkedForDelete() {
		return markedForDelete;
	}

	public void setMarkedForDelete(boolean markedForDelete) {
		this.markedForDelete = markedForDelete;
	}

	@ManyToMany
	public Set<Person> getMembers() {
		return Collections.unmodifiableSet(members);
	}
	
	/**
	 * Only hibernate should call this probably, use 
	 * addMember()
	 * @param members
	 */
	protected void setMembers(Set<Person> members) {
		this.members = members;
	}
	
	public void addMember(Person person) {
		members.add(person);
	}
	
	public void addMembers(Set<Person> persons) {
		members.addAll(persons);
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
