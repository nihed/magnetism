package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.persistence.User;
import com.dumbhippo.tx.RetryException;

@Local
public interface MySpacePersonBlockHandler extends SingleBlockForFeedBlockHandler {
	public void migrate(User user) throws RetryException;	
}