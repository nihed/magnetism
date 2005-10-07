package com.dumbhippo.server;

import javax.ejb.Remote;

/**
 * This doesn't derive from AuthenticationSystem because getServerSecret()
 *  is not something we want to remote...
 * 
 * @author hp
 *
 */
@Remote
public interface AuthenticationSystemRemote {
	

}
