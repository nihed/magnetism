package com.dumbhippo.server;

import javax.ejb.Local;

@Local
public interface ServerStatus {
	public boolean isTooBusy();
}
