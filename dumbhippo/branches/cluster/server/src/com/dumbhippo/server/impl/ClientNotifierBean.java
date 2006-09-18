package com.dumbhippo.server.impl;

import org.jboss.annotation.ejb.Service;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.UserPrefChangedEvent;
import com.dumbhippo.server.ClientNotifier;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.SimpleServiceMBean;
import com.dumbhippo.server.util.EJBUtil;

@Service
public class ClientNotifierBean implements ClientNotifier, SimpleServiceMBean, LiveEventListener<UserPrefChangedEvent> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ClientNotifierBean.class);

	public void start() throws Exception {
		LiveState.addEventListener(UserPrefChangedEvent.class, this);
	}

	public void stop() throws Exception {
		LiveState.removeEventListener(UserPrefChangedEvent.class, this);
	}

	public void onEvent(UserPrefChangedEvent event) {
		IdentitySpider spider = EJBUtil.defaultLookup(IdentitySpider.class);
		MessageSender sender = EJBUtil.defaultLookup(MessageSender.class);
		sender.sendPrefChanged(spider.lookupUser(event.getUserId()), event.getKey(), event.getValue());
	}
}
