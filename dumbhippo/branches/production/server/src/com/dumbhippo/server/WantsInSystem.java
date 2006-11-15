package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.persistence.WantsIn;
import com.dumbhippo.server.views.WantsInView;

@Local
public interface WantsInSystem {

	public void addWantsIn(String address) throws ValidationException;
	
	public List<WantsIn> getWantsInWithoutInvites(int count);
	
	public List<WantsInView> getWantsInViewsWithoutInvites(int count);
	
	/**
	 * Returns true if the email address is in the wants in list,
	 * regardless of whether the invitation for this e-mail has been marked 
	 * as sent. 
	 * 
	 * @param emailAddress
	 * @return true if the email addres is in the wants in list, false otherwise
	 */
	public boolean isWantsIn(String emailAddress);
	
	public int getWantsInCount();
}
