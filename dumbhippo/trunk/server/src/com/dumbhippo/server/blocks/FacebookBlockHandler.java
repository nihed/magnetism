package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.StackInclusion;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.ExternalAccountsListener;
import com.dumbhippo.server.listeners.FacebookListener;

@Local
public interface FacebookBlockHandler
	extends BlockHandler, ExternalAccountsListener, FacebookListener {
	public BlockKey getKey(User user, StackInclusion inclusion);
	public BlockKey getKey(Guid userId, StackInclusion inclusion);
}
