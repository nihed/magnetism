package com.dumbhippo.server;

import javax.ejb.Local;

@Local
public interface AjaxGlue extends AjaxGlueXmlRpc {
	public void init(String personId, String authCookie);
}
