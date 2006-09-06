package com.dumbhippo.mbean;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;

import org.jboss.cache.CacheException;
import org.jboss.cache.RegionNotEmptyException;
import org.jboss.cache.TreeCacheMBean;
import org.jboss.cache.marshall.RegionNameConflictException;
import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.SchemaUpdater;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.impl.AbstractCacheBean;
import com.dumbhippo.server.impl.FeedUpdaterBean;
import com.dumbhippo.server.impl.MusicSystemInternalBean;
import com.dumbhippo.server.impl.TransactionRunnerBean;
import com.dumbhippo.server.util.EJBUtil;

// The point of this extremely simple MBean is to get notification
// when our application is loaded and unloaded; in particular, we
// need to clean up some resources on unload. ServiceMBeanSupport
// has skeleton implementations of the ServiceMBean methods;
// we just need to provide the code to run on start and shutdown.
public class HippoService extends ServiceMBeanSupport implements HippoServiceMBean {
	private static final Logger logger = GlobalSetup.getLogger(HippoService.class);
	
	private Configuration config;
	
	@Override
	protected void startService() {
		logger.info("Starting HippoService MBean");
		config = EJBUtil.defaultLookup(Configuration.class);
		SchemaUpdater.update();
		if (!config.getProperty(HippoProperty.SLAVE_MODE).equals("yes"))
			FeedUpdaterBean.startup();
		
		/* We need to register this context's class loader in order for the JBoss TreeCache
		 * to be able to deserialize enumeration values recieved from other nodes.  
		 * 
		 * See: http://jboss.org/index.html?module=bb&op=viewtopic&p=3968318
		 */
		MBeanServer server = MBeanServerLocator.locateJBoss();
		TreeCacheMBean cache;		
		try {
			cache = (TreeCacheMBean) MBeanProxyExt.create(TreeCacheMBean.class, "jboss.cache:service=EJB3EntityTreeCache", server);
			cache.registerClassLoader("/", Thread.currentThread().getContextClassLoader());
			
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException(e);
		} catch (RegionNameConflictException e) {
			throw new RuntimeException(e);
		}
		
		/*
		 * The cache starts deactivated (per XML config) until we've set the
		 * classloader, activate it now.
		 */
		try {
			cache.activateRegion("/");
		} catch (RegionNotEmptyException e) {
			throw new RuntimeException(e);
		} catch (RegionNameConflictException e) {
			throw new RuntimeException(e);
		} catch (CacheException e) {
			throw new RuntimeException(e);
		}
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
		if (!config.getProperty(HippoProperty.SLAVE_MODE).equals("yes"))		
			FeedUpdaterBean.shutdown();
		TransactionRunnerBean.shutdown();
   }
}
