package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;

/** 
 * This bean checks availability of the blog feeds for MySpace external accounts that did
 * not have the feed set initially because they were private.
 * 
 * @author marinaz
 */
@Local
public interface MySpaceUpdater extends PollingTaskLoader {

	/**
	 * Creates and adds a feed for the MySpace blog for an external account.
	 * 
	 * @param external 
	 * @throws XmlMethodException if the feed for the MySpace blog could not be found,
	 *                            for example, if the MySpace account is private 
	 */
	public void createFeedForMySpaceBlog(ExternalAccount external) throws XmlMethodException;
}