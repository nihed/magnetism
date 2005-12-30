package com.dumbhippo.server;

import javax.ejb.Local;

@Local
public interface WantsInSystem {

	public void addWantsIn(String address);
}
