package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.User;

public interface BlockPerFeedEntryBlockHandler extends BlockHandler {
	public BlockKey getKey(User user, FeedEntry feedEntry);
}
