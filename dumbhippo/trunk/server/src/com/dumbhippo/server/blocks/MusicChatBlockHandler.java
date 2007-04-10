package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.TrackHistory;

@Local
public interface MusicChatBlockHandler
	extends BlockHandler {

	public BlockKey getKey(TrackHistory trackHistory);
}
