package com.dumbhippo.xmppcom;

import java.io.Serializable;

public abstract class XmppTask implements Serializable {

	
	static public final String QUEUE = "OutgoingXMPPQueue";

	@Override
	public String toString() {
		return "{XMPP task}";
	}
}
