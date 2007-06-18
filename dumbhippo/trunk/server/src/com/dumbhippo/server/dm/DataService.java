package com.dumbhippo.server.dm;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMSessionMapJTA;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.dm.JBossInjectableEntityManagerFactory;
import com.dumbhippo.dm.ReadOnlySession;
import com.dumbhippo.dm.ReadWriteSession;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.Viewpoint;

public class DataService extends ServiceMBeanSupport implements DataServiceMBean {
	private static final Logger logger = GlobalSetup.getLogger(DataService.class);
	private DataModel model;
	private EntityManager em;
	
	private static DataService instance;
	
	@Override
	protected void startService() {
		logger.info("Starting DataService MBean");
		EntityManagerFactory emf = new JBossInjectableEntityManagerFactory("java:/DumbHippoManagerFactory");
		
		// Used for flush()
		em = emf.createEntityManager();

		Configuration config = EJBUtil.defaultLookup(Configuration.class);
		String baseUrl = config.getProperty(HippoProperty.BASEURL);
		
		model = new DataModel(baseUrl, new DMSessionMapJTA(), emf, Viewpoint.class, SystemViewpoint.getInstance());
		
		model.addDMClass(ExternalAccountDMO.class);
		model.addDMClass(UserDMO.class);
	
		model.completeDMClasses();
		
		instance = this;
    }
	
    @Override
	protected void stopService() {
		logger.info("Stopping DataService MBean");
		
		instance = null;
    }
    
    public static DataModel getModel() {
    	return instance.model; 
    }
    
    public static ReadWriteSession currentSessionRW() {
    	return instance.model.currentSessionRW();
    }
    
    public static ReadOnlySession currentSessionRO() {
    	return instance.model.currentSessionRO();
    }
    
    /**
     * Flush any pending database operations to the database. This really has
     * nothing to do with the data model, but we already did the work here
     * to dig up a transaction-scoped entity manager. 
     */
    public static void flush() {
    	instance.em.flush();
    }
}
