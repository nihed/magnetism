package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.persistence.User;
import com.dumbhippo.tx.RetryException;

@Local
public interface TwitterPersonBlockHandler extends SingleBlockForFeedBlockHandler {
	public void migrate(User user) throws RetryException;	
}