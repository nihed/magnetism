/**
 * 
 */
package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.HippoAccount;

/**
 * @author hp
 *
 */
@Local
public interface TestGlue {

	public void loadTestData();

	public Set<HippoAccount> getActiveAccounts();
	
	public String authorizeNewClient(long accountId, String name);
	
	public HippoAccount findOrCreateAccountFromEmail(String email);
}
