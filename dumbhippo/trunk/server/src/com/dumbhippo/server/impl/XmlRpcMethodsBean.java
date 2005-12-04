package com.dumbhippo.server.impl;

import java.io.Serializable;

import javax.ejb.Stateless;

import com.dumbhippo.server.XmlRpcMethods;

@Stateless
public class XmlRpcMethodsBean implements XmlRpcMethods, Serializable {
	
	private static final long serialVersionUID = 0L;
	
	public String getStuff() {
		return "This is some stuff!";
	}
}
