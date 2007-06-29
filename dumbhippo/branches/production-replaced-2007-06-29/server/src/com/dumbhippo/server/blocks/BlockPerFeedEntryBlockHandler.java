package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.ExternalAccountFeedListener;
import com.dumbhippo.server.listeners.ExternalAccountsListener;

public interface BlockPerFeedEntryBlockHandler extends BlockHandler, ExternalAccountsListener, ExternalAccountFeedListener {
	public BlockKey getKey(User user, FeedEntry feedEntry);
}
