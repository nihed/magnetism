package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.WantsIn;

@Local
public interface WantsInSystem {

	// internal detail
	public WantsIn findOrCreateWantsIn(String address, boolean increment);
	
	public void addWantsIn(String address);
}
