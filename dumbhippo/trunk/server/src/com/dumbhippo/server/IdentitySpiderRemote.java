package com.dumbhippo.server;

import javax.ejb.Remote;

import com.dumbhippo.FullName;
import com.dumbhippo.persistence.Person;

@Remote
public interface IdentitySpiderRemote extends IdentitySpider {
	
	/**
	 * Person.setName() also exists, and will work in-process
	 * but not remotely (I think). So this thing is only 
	 * needed for remote callers... ?
	 * 
	 * @param person the person
	 * @param name name to give them
	 */
	public void setName(Person person, FullName name);
	
}
