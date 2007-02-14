package com.dumbhippo.server.applications;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

@Local
public interface ApplicationSystem {
	void addUpload(Guid uploaderId, Guid uploadId, AppinfoFile appinfoFile);
	
	ApplicationIconView getIcon(String appId, int size) throws NotFoundException;
}
