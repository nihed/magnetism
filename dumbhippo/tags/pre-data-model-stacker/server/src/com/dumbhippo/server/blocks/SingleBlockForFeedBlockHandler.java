package com.dumbhippo.server.blocks;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.ExternalAccountFeedListener;
import com.dumbhippo.server.listeners.ExternalAccountsListener;

public interface SingleBlockForFeedBlockHandler extends BlockHandler, ExternalAccountsListener, ExternalAccountFeedListener {
	public BlockKey getKey(User user);
	public BlockKey getKey(Guid userId);
}
