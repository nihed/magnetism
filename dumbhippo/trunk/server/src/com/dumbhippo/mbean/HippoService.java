package com.dumbhippo.mbean;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.SchemaUpdater;
import com.dumbhippo.server.impl.AbstractCacheBean;
import com.dumbhippo.server.impl.FeedSystemBean;
import com.dumbhippo.server.impl.MusicSystemInternalBean;
import com.dumbhippo.server.impl.TransactionRunnerBean;

// The point of this extremely simple MBean is to get notification
// when our application is loaded and unloaded; in particular, we
// need to clean up some resources on unload. ServiceMBeanSupport
// has skeleton implementations of the ServiceMBean methods;
// we just need to provide the code to run on start and shutdown.
public class HippoService extends ServiceMBeanSupport implements HippoServiceMBean {
	private static final Logger logger = GlobalSetup.getLogger(HippoService.class);
	
	@Override
	protected void startService() {
		logger.info("Starting HippoService MBean");
		SchemaUpdater.update();
		FeedSystemBean.startup();
    }
	
    @Override
	protected void stopService() {
		logger.info("Stopping HippoService MBean");
		// The order of these matters - if one of them 
		// uses another one, then it will "resurrect" the one it uses,
		// or throw an exception ...
		LiveState.getInstance().shutdown();
		AbstractCacheBean.shutdown();
		MusicSystemInternalBean.shutdown();
		FeedSystemBean.shutdown();
		TransactionRunnerBean.shutdown();
   }
}
