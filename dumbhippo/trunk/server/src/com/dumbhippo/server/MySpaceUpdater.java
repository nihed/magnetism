package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;

/** 
 * This bean checks availability of the blog feeds for MySpace external accounts that did
 * not have the feed set initially because they were private.
 * 
 * @author marinaz
 */
@Local
public interface MySpaceUpdater extends PollingTaskLoader {

}