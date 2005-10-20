package com.dumbhippo.server;

import java.util.List;
import java.util.Set;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Person;

public interface GroupSystem extends LoginRequired {
	public Group createGroup(String name);
	
	public void deleteGroup(Group group);
	
	public void addMember(Group group, Person person);
	
	public List<Group> findGroups();
	
	public List<Group> findGroups(Person viewpoint);	
}
