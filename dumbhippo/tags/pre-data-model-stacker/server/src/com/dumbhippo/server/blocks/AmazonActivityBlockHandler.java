package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.AmazonActivityStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.ExternalAccountsListener;
import com.dumbhippo.server.listeners.AmazonListener;

@Local
public interface AmazonActivityBlockHandler extends BlockHandler, AmazonListener, ExternalAccountsListener {
	public BlockKey getKey(User user, AmazonActivityStatus activityStatus);
}
