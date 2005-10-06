package com.dumbhippo.server;

import javax.ejb.Remote;

@Remote
public interface AuthenticationSystemRemote {

	// This doesn't derive from AuthenticationSystem because getServerSecret()
	// is not something we want to remote...
}
