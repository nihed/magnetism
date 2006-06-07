package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.WantsIn;

@Local
public interface WantsInSystem {

	public void addWantsIn(String address);
	
	public List<WantsIn> getWantsInWithoutInvites(int count);
	
	public List<WantsInView> getWantsInViewsWithoutInvites(int count);
	
	public int getWantsInCount();
}
