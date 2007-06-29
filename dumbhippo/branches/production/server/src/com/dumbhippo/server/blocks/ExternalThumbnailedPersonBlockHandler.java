package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.ExternalAccountsListener;
import com.dumbhippo.tx.RetryException;

public interface ExternalThumbnailedPersonBlockHandler extends BlockHandler, ExternalAccountsListener {
	public BlockKey getKey(User user);
	
	public void migrate(User user) throws RetryException;
}
