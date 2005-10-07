package com.dumbhippo.server;

import javax.ejb.Remote;

@Remote
public interface TestGlueRemote {

	public void loadTestData();
	
}
