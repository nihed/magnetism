package com.dumbhippo.server.dm;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMSessionMapJTA;
import com.dumbhippo.dm.DataModel;
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
	
	private static DataService instance;
	
	@Override
	protected void startService() {
		logger.info("Starting DataService MBean");
		EntityManagerFactory emf;

		try {
			Context context = new InitialContext();
			emf = (EntityManagerFactory)context.lookup("java:/DumbHippoManagerFactory");
		} catch (NamingException e) {
			throw new RuntimeException("Can't get entity manager factory", e);
		}

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
}
