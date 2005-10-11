package com.dumbhippo.server.impl;

import java.io.Serializable;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.server.AjaxGlue;

@Stateful
public class AjaxGlueBean implements AjaxGlue, Serializable {
	
	private static final long serialVersionUID = 0L;
	
	private String personId = null;
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	public void init(String personId) {
		this.personId = personId;
	}
	
	public String getStuff() {
		return "This is some stuff!";
	}
}
