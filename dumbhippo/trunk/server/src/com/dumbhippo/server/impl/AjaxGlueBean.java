package com.dumbhippo.server.impl;

import java.io.Serializable;
import java.util.List;

import javax.ejb.Stateful;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.server.AjaxGlue;
import com.dumbhippo.server.InvitationSystem;

@Stateful
public class AjaxGlueBean implements AjaxGlue, Serializable {
	
	private static final long serialVersionUID = 0L;
	
	private String personId = null;
	private String authCookie = null;
	private boolean authorized = false;
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	public void init(String personId, String authCookie) {
		this.personId = personId;
		this.authCookie = authCookie;
	}
	
	public String getStuff() {
		return "This is some stuff!";
	}
}
