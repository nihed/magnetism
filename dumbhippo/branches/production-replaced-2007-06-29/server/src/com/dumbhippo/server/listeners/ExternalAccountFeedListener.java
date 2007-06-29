package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.User;

public interface ExternalAccountFeedListener {
	public void onExternalAccountFeedEntry(User user, ExternalAccount external, FeedEntry entry, int entryPosition);
}
