package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.ExternalAccountsListener;
import com.dumbhippo.server.listeners.PicasaListener;

@Local
public interface PicasaBlockHandler extends BlockHandler, PicasaListener, ExternalAccountsListener {
	public BlockKey getKey(User user);
	
	public void migrate(User user);
}
