package com.dumbhippo.mbean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

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
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.impl.MusicSystemBean;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.util.FaviconCache;
import com.dumbhippo.services.caches.AbstractCacheBean;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxUtils;

// The point of this extremely simple MBean is to get notification
// when our application is loaded and unloaded; in particular, we
// need to clean up some resources on unload. ServiceMBeanSupport
// has skeleton implementations of the ServiceMBean methods;
// we just need to provide the code to run on start and shutdown.
public class HippoService extends ServiceMBeanSupport implements HippoServiceMBean {
	private static final Logger logger = GlobalSetup.getLogger(HippoService.class);
	private Thread heartbeatThread;
	
	@Override
	protected void startService() {
		logger.info("Starting HippoService MBean");
		SchemaUpdater.update();
		
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
		
		heartbeatThread = new Thread(new Heartbeat());
		heartbeatThread.start();
		
		AccountSystem accounts = EJBUtil.defaultLookup(AccountSystem.class);
		try {
			accounts.createCharacters();
		} catch (RetryException e) {
			throw new RuntimeException("Failed to create characters", e);
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
		MusicSystemBean.shutdown();
		TxUtils.shutdown();
		FaviconCache.shutdown();
		
		heartbeatThread.interrupt();
		try {
			heartbeatThread.join();
		} catch (InterruptedException e) {
			logger.warn("Can't stop heartbeat thread");
		}
   }
    
    private static class Heartbeat implements Runnable {
    	public void run() {
    		Calendar calendar = Calendar.getInstance();

    		Formatter formatter = new Formatter();
    		formatter.format("%04d%02d%02d-%02d:%02d:%02d.heartbeat",
    					     calendar.get(Calendar.YEAR),
    					     calendar.get(Calendar.MONTH) + 1,
    					     calendar.get(Calendar.DAY_OF_MONTH),
    					     calendar.get(Calendar.HOUR_OF_DAY),
    					     calendar.get(Calendar.MINUTE),
    					     calendar.get(Calendar.SECOND));
    		
    		File file = new File(formatter.toString());
    		
    		try {
				OutputStream ostream = new FileOutputStream(file);
				Writer writer = new OutputStreamWriter(ostream);
				long lastTime = System.currentTimeMillis();
				
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						break;
					}
					long newTime = System.currentTimeMillis();
					Long slept = newTime - lastTime;
					lastTime = newTime;
					writer.write(new Date() + " " + slept + "\n");
					writer.flush();
				}
				
				writer.close();
			} catch (IOException e) {
			}
    	}
    }
}
