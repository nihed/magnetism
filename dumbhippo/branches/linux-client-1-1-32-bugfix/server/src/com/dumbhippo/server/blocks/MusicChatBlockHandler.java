package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.server.listeners.MusicChatListener;

@Local
public interface MusicChatBlockHandler
	extends BlockHandler, MusicChatListener {

	public BlockKey getKey(TrackHistory trackHistory);
}
