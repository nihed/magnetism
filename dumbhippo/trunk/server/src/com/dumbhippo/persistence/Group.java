package com.dumbhippo.persistence;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name="HippoGroup") // "Group" is a sql command so default name breaks things
public class Group extends GuidPersistable {
	private static final long serialVersionUID = 1L;
	
	private GroupType type;
	private String name;
	private Set<Person> members;
	private boolean markedForDelete;
		
	private void initMissing() {
		if (type == null)
			type = GroupType.MEMBERS_ONLY;
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
	
	@Column(nullable=false)
	public GroupType getType() {
		return type;
	}

	public void setType(GroupType type) {
		this.type = type;
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
	
	public void removeMember(Person person) {
		// Silently do nothing if already not a member
		members.remove(person);
	}
	
	@Column(nullable=false)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Convert an (unordered) set of groups into a a list and
	 * sort alphabetically with the default collator. You generally
	 * want to do this before displaying things to user, since
	 * iteration through Set will be in hash table order.
	 * 
	 * @param groups a set of Group objects
	 * @return a newly created List containing the sorted groups
	 */
	static public List<Group> sortedList(Set<Group> groups) {
		ArrayList<Group> list = new ArrayList<Group>();
		list.addAll(groups);

		final Collator collator = Collator.getInstance();
		Collections.sort(list, new Comparator<Group>() {
			public int compare (Group g1, Group g2) {
				return collator.compare(g1.getName(), g2.getName());
			}
		});
		
		return list;
	}
}
