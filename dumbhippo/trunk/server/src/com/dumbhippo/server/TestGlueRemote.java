package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Remote;

import com.dumbhippo.persistence.HippoAccount;

@Remote
public interface TestGlueRemote {

	public void loadTestData();

	public Set<HippoAccount> getActiveAccounts();
	
	public String authorizeNewClient(HippoAccount account, String name);
}
