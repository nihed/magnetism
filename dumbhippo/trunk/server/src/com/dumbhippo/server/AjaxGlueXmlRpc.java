package com.dumbhippo.server;

import java.util.List;



/**
 * Methods in this interface are exposed via the Internet!
 * 
 * If you need an inside-our-web-app-only method, add it to the 
 * AjaxGlue interface that extends this one.
 * 
 * @author hp
 *
 */
public interface AjaxGlueXmlRpc {

	public String getStuff();

	List<String> getFriendCompletions(String entryContents);
}