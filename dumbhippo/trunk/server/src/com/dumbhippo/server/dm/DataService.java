package com.dumbhippo.server.dm;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.dm.ChangeNotificationSet;
import com.dumbhippo.dm.ChangeNotifier;
import com.dumbhippo.dm.DMSessionMapJTA;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.dm.JBossInjectableEntityManagerFactory;
import com.dumbhippo.dm.ReadOnlySession;
import com.dumbhippo.dm.ReadWriteSession;
import com.dumbhippo.jms.JmsConnectionType;
import com.dumbhippo.jms.JmsConsumer;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.jms.JmsShutdownException;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.Viewpoint;

public class DataService extends ServiceMBeanSupport implements DataServiceMBean, ChangeNotifier {
	static public final String TOPIC_NAME = "topic/DataModelTopic";

	private static final Logger logger = GlobalSetup.getLogger(DataService.class);
	private DataModel model;
	private EntityManager em;
	private JmsProducer topicProducer;
	private JmsConsumer topicConsumer;
	private Thread topicConsumerThread;
	
	private static DataService instance;
	
	// This service is started before HippoService, which sets up the tree cache and updates db schemas.
	// Thus, don't try to use the database in here.
	@Override
	protected void startService() {
		logger.info("Starting DataService MBean");
		EntityManagerFactory emf = new JBossInjectableEntityManagerFactory("java:/DumbHippoManagerFactory");
		
		// Used for flush()
		em = emf.createEntityManager();

		Configuration config = EJBUtil.defaultLookup(Configuration.class);
		
		// FIXME using the Mugshot base url hardcoded here is not good. config.getBaseUrl() 
		// will whine about it once in the logs so we don't forget.
		String baseUrl = config.getBaseUrl(Site.NONE);
		
		model = new DataModel(baseUrl, new DMSessionMapJTA(), emf, this, Viewpoint.class, SystemViewpoint.getInstance());
		
		model.addDMClass(ApplicationDMO.class);
		model.addDMClass(DesktopSettingDMO.class);
		model.addDMClass(ExternalAccountDMO.class);
		model.addDMClass(TrackDMO.class);
		model.addDMClass(UserDMO.class);
		model.addDMClass(ContactDMO.class);
		
		model.completeDMClasses();
		
		topicProducer = new JmsProducer(TOPIC_NAME, JmsConnectionType.NONTRANSACTED_IN_SERVER);
		
		topicConsumer = new JmsConsumer(TOPIC_NAME, JmsConnectionType.NONTRANSACTED_IN_SERVER);
		topicConsumerThread = new Thread(new TopicConsumer(), "IndexQueueConsumer");
		topicConsumerThread.start(); 
		
		instance = this;
    }
	
    @Override
	protected void stopService() {
		logger.info("Stopping DataService MBean");

		topicConsumer.shutdown(); // Will stop consumer thread as a side effect
		
		try {
			topicConsumerThread.join(30 * 1000);
			if (topicConsumerThread.isAlive())
				logger.warn("Timed out waiting for IndexQueue consumer thread to shut down");
		} catch (InterruptedException e) {
			// Shouldn't happen
		}

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

	public void broadcastNotify(ChangeNotificationSet notifications) {
		topicProducer.sendObjectMessage(notifications);
	}
	
	private class TopicConsumer implements Runnable {
		public void run() {
			while (true) {
				try {
					Message message = topicConsumer.receive();
					if (!(message instanceof ObjectMessage)) {
						logger.warn("Got unexpected type of message in queue.");
						continue;
					}
					
					Object object;
					try {
						object = ((ObjectMessage)message).getObject();
					} catch (JMSException e) {
						logger.warn("Error retrieving object from queue.", e);
						continue;
					}
					
					if (!(object instanceof ChangeNotificationSet)) {
						logger.warn("Got unexpected object type in queue {}", object.getClass());
						continue;
					}
					
					String sourceAddress;
					try {
						sourceAddress = message.getStringProperty("sourceAddress");
					} catch (JMSException e) {
						logger.warn("Can't get source address");
						continue;
					}
					String localAddress = System.getProperty("jboss.bind.address");
					boolean isLocal = localAddress.equals(sourceAddress);
					
					if (!isLocal)
						model.notifyRemoteChange((ChangeNotificationSet)object);
	
				} catch (JmsShutdownException e) {
					logger.debug("Topic was shut down, exiting thread");
					break;
				} catch (RuntimeException e) {
					logger.error("Unexpected error receiving dispatching messages", e);
				}
			}
		}
	}
}
