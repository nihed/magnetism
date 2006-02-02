package com.dumbhippo.live;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.server.IdentitySpider;

// Implementatin of LiveUserUpdater
@Stateless
public class LiveUserUpdaterBean implements LiveUserUpdater {
	@EJB
	IdentitySpider identitySpider;
	
	public void initialize(LiveUser user) {
	}
}
