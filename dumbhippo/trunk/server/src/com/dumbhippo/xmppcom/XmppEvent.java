package com.dumbhippo.xmppcom;

import java.io.Serializable;

public abstract class XmppEvent implements Serializable {

	
	@Override
	public String toString() {
		return "{XMPP event}";
	}
}
