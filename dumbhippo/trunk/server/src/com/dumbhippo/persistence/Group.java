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
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="HippoGroup") // "Group" is a sql command so default name breaks things
public class Group extends GuidPersistable {
	private static final long serialVersionUID = 1L;
	
	private GroupAccess access;
	private String name;
	private Set<GroupMember> members;
	private boolean markedForDelete;
		
	private void initMissing() {
		if (access == null)
			access = GroupAccess.PUBLIC_INVITE;
		if (members == null)
			members = new HashSet<GroupMember>();
	}
	
	public Group() {
		initMissing();
	}
	
	public Group(String name) {
		this.name = name;
		initMissing();
	}
	
	public Group(String name, Set<GroupMember> members) {
		this.name = name;
		setMembers(members);
		initMissing();
	}
	
	@Column(nullable=false)
	public GroupAccess getAccess() {
		return access;
	}

	public void setAccess(GroupAccess type) {
		this.access = type;
	}

	protected boolean isMarkedForDelete() {
		return markedForDelete;
	}

	public void setMarkedForDelete(boolean markedForDelete) {
		this.markedForDelete = markedForDelete;
	}

	@OneToMany(mappedBy="group")
	public Set<GroupMember> getMembers() {
		return members;
	}
	
	/**
	 * Only hibernate should call this probably, use 
	 * addMember()
	 * @param members
	 */
	protected void setMembers(Set<GroupMember> members) {
		if (members == null)
			throw new IllegalArgumentException("null");
		this.members = members;
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
	
	@Override
	public String toString() {
		return "{Group " + "guid = " + getId() + " name = " + getName() + " access = " + getAccess() + "}";
	}	
}
