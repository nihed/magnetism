package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.Revision;

public interface RevisionListener {
	public void onRevisionAdded(Revision revision);
}
