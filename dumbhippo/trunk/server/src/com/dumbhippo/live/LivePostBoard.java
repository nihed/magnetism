package com.dumbhippo.live;

import javax.ejb.Local;

import com.dumbhippo.server.Viewpoint;

@Local
public interface LivePostBoard {
	public String getLivePostXML(Viewpoint viewpoint, LivePost post);
}
