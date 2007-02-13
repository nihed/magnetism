package com.dumbhippo.server.applications;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;

@Local
public interface ApplicationSystem {
	void addUpload(Guid uploaderId, Guid uploadId, AppinfoFile appinfoFile);
}
