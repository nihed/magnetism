package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.server.listeners.ExternalAccountsListener;

@Local
public interface ExternalAccountChangePropagator extends ExternalAccountsListener {
}
