package com.dumbhippo.server;

import javax.ejb.Remote;

import com.dumbhippo.identity20.Guid.ParseException;

@Remote
public interface TestGlueRemote extends TestGlue {
	public void setInvitations(String userId, int invites) throws ParseException, NotFoundException;
	
}
