package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;



/**
 * Methods in this interface are exposed via the Internet!
 * 
 * If you need an inside-our-web-app-only method, add it to 
 * a different interface.
 * 
 * @author hp
 *
 */
@Local
public interface AjaxGlueXmlRpc {

	public String getStuff();

	List<String> getFriendCompletions(String entryContents);
}