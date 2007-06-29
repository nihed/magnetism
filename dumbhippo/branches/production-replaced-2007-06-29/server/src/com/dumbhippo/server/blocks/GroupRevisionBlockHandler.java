package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.GroupRevision;
import com.dumbhippo.server.listeners.RevisionListener;

@Local
public interface GroupRevisionBlockHandler extends BlockHandler, RevisionListener {
	public BlockKey getKey(GroupRevision revision);
}
