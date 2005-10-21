package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Person;

@Local
public interface GroupSystem {
	public Group createGroup(Person creator, String name);
	
	public void deleteGroup(Person deleter, Group group);
	
	public void addMember(Person adder, Group group, Person person);
	
	public List<Group> findGroups(Person viewpoint);	
}
