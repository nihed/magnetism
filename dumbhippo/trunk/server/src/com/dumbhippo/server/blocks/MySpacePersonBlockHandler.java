package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.server.listeners.ExternalAccountFeedListener;
import com.dumbhippo.server.listeners.ExternalAccountsListener;

@Local
public interface MySpacePersonBlockHandler extends BlogLikeBlockHandler, ExternalAccountsListener, ExternalAccountFeedListener {
}