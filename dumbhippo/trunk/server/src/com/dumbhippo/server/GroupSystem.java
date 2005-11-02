package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Person;

@Local
public interface GroupSystem {
	public Group createGroup(Person creator, String name);
	
	public void deleteGroup(Person deleter, Group group);
	
	public void addMember(Person adder, Group group, Person person);
	
	public void removeMember(Person remover, Group group, Person person);
	
	public Set<PersonView> getMembers(Viewpoint viewpoint, Group group);
	
	public boolean isMember(Viewpoint viewpoint, Group group, Person member);
	
	public Set<Group> findGroups(Viewpoint viewpoint, Person member);	
}
