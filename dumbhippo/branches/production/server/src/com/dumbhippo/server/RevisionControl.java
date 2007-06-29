package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Revision;

@Local
public interface RevisionControl {

	void persistRevision(Revision revision);
	
}
