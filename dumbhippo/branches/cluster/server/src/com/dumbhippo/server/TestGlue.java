/**
 * 
 */
package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.Account;

/**
 * @author hp
 *
 */
@Local
public interface TestGlue {

	public void loadTestData();

	public Set<Account> getActiveAccounts();
	
	public String authorizeNewClient(String accountId, String name);
}
