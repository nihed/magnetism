package com.dumbhippo.server.impl;

import java.io.Serializable;

import javax.annotation.EJB;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.server.AbstractLoginRequired;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.AjaxGlueXmlRpc;
import com.dumbhippo.server.IdentitySpider;

@Stateful
public class AjaxGlueXmlRpcBean extends AbstractLoginRequired implements AjaxGlueXmlRpc, Serializable {
	
	private static final long serialVersionUID = 0L;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private AccountSystem accountSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	public String getStuff() {
		return "This is some stuff!";
	}
}
