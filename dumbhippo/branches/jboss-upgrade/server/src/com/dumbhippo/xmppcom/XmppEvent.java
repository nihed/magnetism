package com.dumbhippo.xmppcom;

import java.io.Serializable;

public abstract class XmppEvent implements Serializable {

	
	static public final String QUEUE = "IncomingXMPPQueue";

	@Override
	public String toString() {
		return "{XMPP event " + getClass().getName() + "}";
	}
}
