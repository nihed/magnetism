package com.dumbhippo.server.impl;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlueRemote;

@Stateless
public class MessengerGlueBean implements MessengerGlueRemote {
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	

	public boolean authenticateJabberUser(String username, String token, String digest) {
		
		// TODO do some stuff here and maybe return true
		
		return false;
	}
	

	public long getJabberUserCount() {
		// TODO Auto-generated method stub
		return 0;
	}


	public void setName(String username, String name)
		throws JabberUserNotFoundException {
		// TODO Auto-generated method stub
		
	}


	public void setEmail(String username, String email) 
		throws JabberUserNotFoundException {
		// TODO Auto-generated method stub
		
	}


}
