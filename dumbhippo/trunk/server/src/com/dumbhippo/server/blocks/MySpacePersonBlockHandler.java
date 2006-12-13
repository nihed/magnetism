package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.persistence.User;

@Local
public interface MySpacePersonBlockHandler extends SingleBlockForFeedBlockHandler {
	public void migrate(User user);	
}