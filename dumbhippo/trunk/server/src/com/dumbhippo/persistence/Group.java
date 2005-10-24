package com.dumbhippo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name="HippoGroup") // "Group" is a sql command so default name breaks things
public class Group extends GuidPersistable {
	private static final long serialVersionUID = 1L;
	
	private String name;
	private Set<Person> members;
	private boolean markedForDelete;
		
	private void initMissing() {
		if (members == null)
			members = new HashSet<Person>();
	}
	
	public Group() {
		initMissing();
	}
	
	public Group(String name) {
		this.name = name;
		initMissing();
	}
	
	public Group(String name, Set<Person> members) {
		this.name = name;
		setMembers(members);
		initMissing();
	}
	
	protected boolean isMarkedForDelete() {
		return markedForDelete;
	}

	public void setMarkedForDelete(boolean markedForDelete) {
		this.markedForDelete = markedForDelete;
	}

	@ManyToMany
	public Set<Person> getMembers() {
		return members;
	}
	
	/**
	 * Only hibernate should call this probably, use 
	 * addMember()
	 * @param members
	 */
	protected void setMembers(Set<Person> members) {
		if (members == null)
			throw new IllegalArgumentException("null");
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
